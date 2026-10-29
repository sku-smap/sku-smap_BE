# 비공개 360 사진 연결

- 버킷: `smap-photos-450974103987-ap-northeast-2-an`
- 리전: `ap-northeast-2`
- 키: DB의 `nodes.original_filename` 그대로. 현재 별도 폴더 없음.
- 기본 URL 유효기간: 3600초. 임시 자격 증명이 먼저 만료되면 URL도 더 일찍 만료될 수 있다.

## 프론트 사진 URL 사용 계약

| 용도 | API | URL 위치 |
| --- | --- | --- |
| 캠퍼스 둘러보기 | `GET /api/v1/campus-map` | `data.nodes[].imageUrl` |
| 경로 안내 | `POST /api/v1/routes` | `data.nodes[].imageUrl` |
| 노드 목록 | `GET /api/v1/nodes` | URL 미제공. 같은 nodeId의 campus-map 항목을 참조 |

- 비공개 S3 객체에 대한 HTTPS GET 전용 Presigned URL이다. 반환된 URL을 추가 인코딩하거나 쿼리 문자열을 제거하지 말고 그대로 이미지·360도 뷰어에 전달한다.
- 사진을 가져올 때 AWS 키나 별도의 Authorization 헤더를 프론트에서 붙이지 않는다. URL 자체에 임시 접근 서명이 포함된다.
- 기본 유효기간은 발급 시점부터 3600초다. `S3_PHOTOS_URL_TTL_SECONDS`로 변경 가능하며 임시 AWS 자격 증명이 먼저 만료되면 URL도 먼저 만료될 수 있다. 현재 응답에는 별도의 만료 시각 필드는 없다.
- 둘러보기 중 갱신하려면 `GET /api/v1/campus-map`을 다시 호출하고 nodeId로 새 URL을 연결한다. 경로는 같은 요청 본문으로 `POST /api/v1/routes`를 재호출할 수 있다. 같은 사진의 URL은 두 API 중 어느 쪽에서 발급받아도 사용할 수 있다.
- 캠퍼스 맵은 Redis 메타데이터 캐시에 적중해도 사진 URL을 매 요청마다 서명한다. 동일한 시각에 발급하면 URL 문자열이 같을 수 있으므로 문자열 변경을 갱신 성공 기준으로 삼지 않는다.
- 장시간 열어둔 화면은 기본 설정 기준 1시간 이내에 재조회한다. 사진 GET이 만료·권한 오류로 실패하면 URL을 한 번 갱신해 재시도하고, 계속 실패하면 사진 로딩 오류를 표시한다. 무한 재시도하지 않는다.
- `S3_PHOTOS_ENABLED=false`이면 imageUrl은 null이다. 이때 사진 준비 중 안내를 표시하고 S3 요청을 보내지 않는다.
- DB와 Redis에는 임시 URL을 영구 저장하지 않는다. 서명 생성은 객체 존재·조회 권한을 검증하지 않으므로 API가 성공해도 실제 S3 GET은 별도로 실패할 수 있다.

## 사진 유형

사용자 확인에 따라 현재 328개 노드 사진은 모두 360도 파노라마이다.
`GET /api/v1/nodes`의 `data[]`, `GET /api/v1/campus-map` 및
`POST /api/v1/routes`의 `data.nodes[]`에 `photoType: "PANORAMA"`를 제공한다.
현재 허용값은 `PANORAMA` 하나이며 일반 사진 노드는 없다. 파일명으로 유형을 추정하지 않는다.
장소 사진은 2D 일반 사진이지만 API 응답으로 제공하지 않는다.
`/places`, `/campus-map`, `/routes`의 장소 정보에서 `doorPhoto`를 제외하며
장소 사진 유형 필드도 제공하지 않는다. DB의 door_photo 값은 그대로 보존한다.
DB 테이블·컬럼 추가 없이 응답 DTO에서 제공한다.

```json
{
  "nodeId": "field",
  "originalFilename": "스콘스퀘어.jpg",
  "photoType": "PANORAMA",
  "imageUrl": "<GET 전용 Presigned URL>"
}
```

`GET /api/v1/nodes`에는 위 예시의 imageUrl 필드가 없다.

## 자격 증명과 권한

`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 로컬 `.env` 또는 실행 환경에 설정하면 Spring 설정을 통해 서명기에 주입한다. 두 값은 함께 설정해야 한다. 실제 키는 Git에서 제외된 `.env`에만 저장하며 소스·SQL·프론트 코드에 넣지 않는다. 키를 지정하지 않으면 SDK 기본 자격 증명 체인으로 AWS 프로필·실행 환경의 IAM 역할 등을 사용한다. EC2 적용 시에는 이 로컬 파일 수정이 자동 배포되는 것이 아니므로 서버 실행 환경에도 별도로 설정해야 한다.

실행 주체에 필요한 읽기 권한 예시:
임시 자격 증명을 직접 주입할 때는 `AWS_SESSION_TOKEN`도 함께 설정한다. 토큰이 있으면 세션 자격 증명으로, 없으면 일반 액세스 키로 서명한다. 세 값 모두 없는 경우에는 기존 AWS 기본 자격 증명 체인을 사용한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::smap-photos-450974103987-ap-northeast-2-an/*"
  }]
}
```

AWS CLI를 사용하는 로컬 환경은 `aws configure --profile smap` 등으로 프로필을 설정하고, 서버 실행 프로세스에 `AWS_PROFILE=smap`을 설정한다. 운영 컨테이너도 역할 자격 증명에 접근할 수 있어야 한다.

S3 웹 뷰어가 fetch/WebGL로 사진을 읽으면 S3 CORS에도 프론트 Origin의 GET을 허용해야 한다. 실제 프론트 주소가 확정된 후 버킷 CORS에 해당 Origin을 등록한다. 버킷의 모든 퍼블릭 액세스 차단은 유지한다.

설정은 `.env.example`의 `S3_PHOTOS_*`를 참고한다. AWS 설정 전 경로만 테스트하려면 `S3_PHOTOS_ENABLED=false`로 실행하면 imageUrl은 null이다. 활성화 상태에서 자격 증명을 읽지 못하면 공통 오류 `PHOTO_503_SIGNING_FAILED`를 반환한다.

AWS 공식 구현 참고: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html
