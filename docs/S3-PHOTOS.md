# 비공개 360 사진 연결

- 버킷: `smap-photos-450974103987-ap-northeast-2-an`
- 리전: `ap-northeast-2`
- 키: DB의 `nodes.original_filename` 그대로. 현재 별도 폴더 없음.
- 기본 URL 유효기간: 3600초. 임시 자격 증명이 먼저 만료되면 URL도 더 일찍 만료될 수 있다.

`POST /api/v1/routes`의 `data.nodes[].imageUrl`로 GET 전용 Presigned URL을 반환한다. URL을 별도로 인코딩하지 말고 그대로 뷰어에 전달한다. 만료되면 경로를 다시 조회하여 새 URL을 받는다. DB에는 파일명만 유지한다. 서명 생성은 객체 존재·조회 권한을 검증하지 않으므로 실제 URL GET으로 확인해야 한다.

## 자격 증명과 권한

`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 로컬 `.env` 또는 실행 환경에 설정하면 Spring 설정을 통해 서명기에 주입한다. 두 값은 함께 설정해야 한다. 실제 키는 Git에서 제외된 `.env`에만 저장하며 소스·SQL·프론트 코드에 넣지 않는다. 키를 지정하지 않으면 SDK 기본 자격 증명 체인으로 AWS 프로필·실행 환경의 IAM 역할 등을 사용한다. EC2 적용 시에는 이 로컬 파일 수정이 자동 배포되는 것이 아니므로 서버 실행 환경에도 별도로 설정해야 한다.

실행 주체에 필요한 읽기 권한 예시:

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
