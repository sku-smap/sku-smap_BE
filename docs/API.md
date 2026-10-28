# SMAP 길찾기·AI 검색 API 명세

현재 컨트롤러·DTO·경로 정책·예외 처리 구현을 기준으로 작성한 문서다.

## 공통 정보

| 항목 | 값 |
|---|---|
| 로컬 Base URL | `http://localhost:8080` — 포트 변경 시 해당 포트 사용 |
| 인증 | 애플리케이션 로그인·토큰 불필요 |
| 요청·응답 형식 | `application/json` (음성 검색만 `multipart/form-data`) |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |

현재 `prod` 프로필에서는 Swagger UI·OpenAPI JSON이 비활성화된다. 운영 Base URL은 배포 주소를 사용한다.

## 경로 조회

```http
POST /api/v1/routes
Content-Type: application/json
```

DB의 방향 간선을 이용해 조건별 비용 합이 최소인 경로를 계산한다. 없는 노드·역방향 간선을 생성하지 않는다.

### 요청

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `fromNodeId` | string | O | 실제 출발 노드 ID. null·빈 문자열·공백만 있는 값 불가 |
| `toNodeId` | string | O | 실제 도착 노드 ID. null·빈 문자열·공백만 있는 값 불가 |
| `option` | string 또는 null | X | 단일 이동 조건. 생략·null이면 `OUTDOOR_FIRST` |

ID·옵션은 대소문자를 구분한다. 장소 검색 API에서 장소를 선택한 뒤 해당 장소의 `arriveNodeId`를 사용한다. 경로 요청에 장소 이름·place ID를 직접 입력하는 기능은 제공하지 않는다.

```json
{
  "fromNodeId": "bus_stop",
  "toNodeId": "field",
  "option": "COMFORTABLE"
}
```

### 이동 조건

| option | 이름 | 간선 비용 |
|---|---|---|
| `OUTDOOR_FIRST` | 야외경로 우선 — 기본값 | 기본 1 + 실내·지하 끝점마다 1 |
| `COMFORTABLE` | 편한길 | 기본 1 + 계단이면 5 + 야외·지하 끝점마다 1 |
| `SHORTEST` | 최단거리 | 모든 간선 1 |
| `INDOOR_UNDERGROUND_FIRST` | 실내지하우선 | 기본 1 + 야외 끝점마다 1 |

끝점은 간선의 출발·도착 노드이다. 비선호 공간끼리 이동하면 공간 비용 +2, 실내·야외 전환이면 +1이다. 편한길에서 실내 도보는 1, 야외·지하 도보는 3, 실내 계단은 6, 야외·지하 계단은 8이다.

- 최단거리는 실제 거리가 아닌 이동 간선 수를 최소화한다.
- 지하층은 편한길에서 야외로, 실내지하우선에서 실내로 취급한다.
- 공간은 건물·층·사진명과 확인한 외부 출입구 목록으로 분류한다. 불명확한 공간은 공간 추가 비용 0이다.
- 계단은 금지하지 않는다. 우회 비용이 더 크면 편한길에서도 계단을 선택할 수 있다.
- 여러 옵션 동시 선택은 지원하지 않는다. 배열·정의되지 않은 옵션은 400 오류이다.
- 시간·거리·경사도는 계산하지 않으며 가중치 합은 별도 응답 필드로 제공하지 않는다.

## 성공 응답

HTTP `200 OK`. 아래는 `field` → `hill_field_entrance` 요청의 형식 예시다. 사진 URL 문자열은 자리표시자이며 실제로 호출할 수 없다.

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": {
    "nodes": [
      {
        "nodeId": "field",
        "building": "야외",
        "floorLabel": null,
        "title": "스콘스퀘어",
        "originalFilename": "스콘스퀘어.jpg",
        "imageUrl": "<스콘스퀘어 사진의 Presigned URL>",
        "description": "폭풍의언덕 스콘스퀘어 입구로 이동합니다.",
        "places": []
      },
      {
        "nodeId": "hill_field_entrance",
        "building": "야외",
        "floorLabel": null,
        "title": "폭풍의언덕 운동장 입구",
        "originalFilename": "폭풍의언덕_운동장_입구.jpg",
        "imageUrl": "<입구 사진의 Presigned URL>",
        "description": null,
        "places": []
      }
    ],
    "segments": [
      {
        "fromNodeId": "field",
        "toNodeId": "hill_field_entrance",
        "moveType": "WALK",
        "description": "폭풍의언덕 스콘스퀘어 입구로 이동합니다."
      }
    ],
    "totalSteps": 1
  }
}
```

### 응답 최상위·data

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 성공 여부 |
| `code` | string | 성공은 `"200"`, 실패는 오류 코드 |
| `message` | string | 처리 결과 메시지 |
| `data` | object 또는 null | 성공 데이터. 실패 시 null |
| `data.nodes` | Node[] | 출발부터 도착까지 실제 사진 노드 목록 |
| `data.segments` | Segment[] | 화면용 이동 구간 목록. 연속 엘리베이터는 통합 |
| `data.totalSteps` | integer | 통합 전 실제 간선 수. 가중치 합·시간·거리가 아님 |

출발과 도착이 같으면 노드 한 개, 해당 장소 목록, 빈 segments, totalSteps 0을 반환한다. 사진 기능이 켜져 있으면 이 경우에도 AWS 자격 증명이 필요하다.

### Node

| 필드 | 타입 | 설명 |
|---|---|---|
| `nodeId` | string | 노드 ID |
| `building` | string | 건물명 또는 야외 구분 |
| `floorLabel` | string 또는 null | `1`, `B1`, `L` 등 원본 층 표기 |
| `title` | string | 원본 파일명에서 확장자를 제거하고 밑줄을 공백으로 바꾼 이름 |
| `originalFilename` | string | 원본 사진 파일명. 현재 S3 객체 키와 동일 |
| `imageUrl` | string 또는 null | GET 전용 임시 URL. 사진 기능 비활성화 시 null |
| `description` | string 또는 null | 다음 구간 안내. 최종 노드·통합 엘리베이터 중간 노드는 null |
| `places` | Place[] | 해당 노드를 도착 노드로 사용하는 장소들. 없으면 빈 배열 |

장소는 이름·ID 순서로 정렬한다. 경로 중간 노드에 연결된 장소도 모두 반환하며 특정 목적지 하나만을 뜻하지 않는다.

### Place

| 필드 | 타입 | 설명 |
|---|---|---|
| `placeId` | string | 장소 ID |
| `name` | string | 장소 이름 |
| `kind` | string 또는 null | room, office, facility 등 원본 분류 |
| `building` | string 또는 null | 건물명 |
| `floorLabel` | string 또는 null | 장소 층 표기 |
| `roomNo` | string 또는 null | 호실 번호 |
| `arriveNodeId` | string 또는 null | 도착 노드 ID |
| `arriveNodeTitle` | string 또는 null | 도착 노드의 원본 표시 이름 |
| `doorPhoto` | string 또는 null | 출입문 사진 경로. 현재 URL 발급 대상이 아님 |
| `aliases` | string 또는 null | 원본 별명 문자열. 배열이 아님 |
| `sub` | string 또는 null | 부가 설명 |

장소 항목 예시:

```json
{
  "placeId": "r-eunju1-202",
  "name": "도시공학과 실습실 (202호)",
  "kind": "room",
  "building": "은주1관",
  "floorLabel": "2",
  "roomNo": "202",
  "arriveNodeId": "EJ1_02_E",
  "arriveNodeTitle": "은주1관 2층 엘리베이터 앞",
  "doorPhoto": "door/IMG_5807.jpg",
  "aliases": "도시공학과 실습실 | 202호 | 은주1관 202",
  "sub": "은주1관 2층 202호"
}
```

### Segment

| 필드 | 타입 | 설명 |
|---|---|---|
| `fromNodeId` | string | 화면용 구간 출발 노드 |
| `toNodeId` | string | 화면용 구간 도착 노드 |
| `moveType` | string 또는 null | `WALK`, `STAIRS`, `ELEVATOR`, `DOOR`. 원본 빈 값은 null |
| `description` | string 또는 null | 일반 구간은 DB 안내, 통합 엘리베이터는 생성한 안내 |

## 엘리베이터 통합·프론트 처리

실제 경로가 `유담관 L층 → 9층 → 10층 → 10층 복도`이면 segments는 다음 두 개로 반환한다.

```json
[
  {
    "fromNodeId": "s1013_103540",
    "toNodeId": "s1013_105513",
    "moveType": "ELEVATOR",
    "description": "엘리베이터를 타고 L층에서 10층으로 이동합니다."
  },
  {
    "fromNodeId": "s1013_105513",
    "toNodeId": "s1013_105549",
    "moveType": "WALK",
    "description": "복도를 따라 유담관 10층 도서관쪽 복도로 이동합니다."
  }
]
```

- nodes에는 중간 9층을 포함한 네 노드와 사진·장소 정보를 유지한다.
- 승차 노드에는 통합 안내, 중간 엘리베이터 노드에는 null, 하차 노드에는 다음 도보 안내를 제공한다.
- 위 예시의 totalSteps는 실제 간선 수인 3이며 segments.length는 2이다.
- nodes와 segments의 같은 인덱스가 항상 대응하지 않는다. 이동 안내는 segments 순서로 처리하고 사진은 nodeId로 찾는다. 승하차 사진만 표시하려면 구간의 fromNodeId·toNodeId를 사용한다.
- 도보 환승·건물 변경·층 정보 누락·판단 가능한 상승/하강 방향 전환은 분리한다.
- 통합은 응답 표현에만 적용하며 DB에 직접 연결 간선을 추가하지 않는다.

## 사진 URL

- imageUrl을 추가 인코딩하지 말고 그대로 이미지·360도 뷰어에 전달한다.
- 기본 유효기간은 3600초이며 서버 설정으로 변경 가능하다. 임시 AWS 자격 증명이 먼저 만료되면 더 일찍 만료될 수 있다.
- URL 만료 시 경로를 다시 조회한다. DB에 임시 URL을 저장하지 않는다.
- 서명 생성은 객체 존재·조회 권한을 확인하지 않는다. API 성공 후에도 S3 이미지 GET은 권한·객체 키·만료 상태에 따라 실패할 수 있다.
- 브라우저 fetch/WebGL 조회는 S3 CORS 설정도 필요하다.
- 설정·권한은 [S3-PHOTOS.md](S3-PHOTOS.md)를 참고한다.

## 오류 응답

HTTP 상태와 code를 함께 확인한다. 실패 시 data는 null이다.

```json
{
  "success": false,
  "code": "ROUTE_404_NODE",
  "message": "출발 또는 도착 노드를 찾을 수 없습니다.",
  "data": null
}
```

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `COMMON_400_VALIDATION` | 필수 ID 누락·null·빈 문자열·공백 |
| 400 | `COMMON_400_BODY_NOT_READABLE` | JSON 오류, 본문 없음, 잘못된 option·배열 |
| 404 | `ROUTE_404_NODE` | 출발·도착 노드가 DB에 없음 |
| 404 | `ROUTE_404_PATH` | 양 끝 노드는 있지만 저장된 방향으로 도달 불가 |
| 405 | `COMMON_405_METHOD_NOT_ALLOWED` | 지원하지 않는 메서드 |
| 415 | `COMMON_415_UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 요청 Content-Type |
| 500 | `GRAPH_500_NODE_REFERENCE` | 그래프 구성 중 잘못된 노드 참조 발견 |
| 500 | `COMMON_500` | 기타 처리되지 않은 서버 오류 |
| 503 | `COMMON_503_DATABASE_UNAVAILABLE` | DB 연결 불가 등 일시적 접근 오류 |
| 503 | `PHOTO_503_SIGNING_FAILED` | URL 발급용 AWS 자격 증명 등을 사용할 수 없음 |

사진 기능 활성화 상태에서 URL 발급이 실패하면 전체 요청을 오류로 반환한다. 비활성화하면 경로는 반환하고 imageUrl은 null이다. S3 직접 GET 오류는 백엔드 공통 응답 형식과 다르다.

## 호출 방법

Postman에서는 POST 선택 후 Body → raw → JSON에 요청 예시를 입력한다.

Windows PowerShell:

```powershell
$body = @{
    fromNodeId = 'bus_stop'
    toNodeId = 'field'
    option = 'COMFORTABLE'
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/routes' `
    -ContentType 'application/json' `
    -Body $body
```

| 검증 사례 | 기대 결과 |
|---|---|
| 같은 출발·도착으로 네 조건 비교 | 비용에 따라 경로 선택. 결과가 같을 수도 있음 |
| option 생략·null | OUTDOOR_FIRST |
| 같은 출발·도착 ID | 노드 1개, 빈 segments, totalSteps 0 |
| 존재하지 않는 노드 | ROUTE_404_NODE |
| 필수 ID 누락 | COMMON_400_VALIDATION |
| option에 INVALID | COMMON_400_BODY_NOT_READABLE |
| 연속 엘리베이터 뒤 도보 | 승하차 구간 통합, 도보 안내 유지 |

실제 거리·시간·경사도 계산, 옵션 중복 선택, 장소 ID 직접 경로 요청, URL 갱신 전용 API, 4K 변환·CDN은 현재 구현 범위가 아니다.

## 장소 검색 API

### 요청 파라미터 (Request Parameters)

| Parameter | Type | Required | Description |
|---|---|---|---|
| keyword | String | No | Query Parameter. 장소 이름·별명·건물·호실의 부분 일치 검색어 |

검색어를 생략하거나 공백만 보내면 실제 노드에 연결된 전체 장소를 반환한다. 앞뒤 공백은 제거하고 영문 대소문자는 구분하지 않는다. `%`, `_`는 와일드카드가 아닌 일반 문자로 검색한다. 각 필드 안에서 입력 문자열을 부분 검색하며 여러 단어를 분리해서 검색하지는 않는다.

### 응답 (Response)

- 200 OK: 검색 성공. 결과가 없으면 data는 빈 배열
- 503 Service Unavailable: 데이터베이스 접근 실패
- 500 Internal Server Error: 기타 서버 오류

요청 URL:

```http
GET /api/v1/places?keyword=도시공학과%20실습실
```

위 URL의 검색어는 표시용 예시다. 실제 요청에서는 URLSearchParams, Postman Params 등으로 한글과 공백을 URL 인코딩한다.

응답 예시:

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "placeId": "r-eunju1-202",
      "name": "도시공학과 실습실 (202호)",
      "kind": "room",
      "building": "은주1관",
      "floorLabel": "2",
      "roomNo": "202",
      "arriveNodeId": "EJ1_02_E",
      "arriveNodeTitle": "은주1관 2층 엘리베이터 앞",
      "doorPhoto": "door/IMG_5807.jpg",
      "aliases": "도시공학과 실습실 | 202호 | 은주1관 202",
      "sub": "은주1관 2층 202호"
    }
  ]
}
```

응답 항목은 위 Place 표와 같은 11개 필드다. 실제 노드가 존재하는 매핑만 반환하므로 arriveNodeId는 이 API에서 비어 있지 않다. 정렬은 이름, placeId 오름차순이다. 페이징은 적용하지 않는다. 현재 데이터 기준 전체 조회는 970개다.

### 헤더 정보 (Headers)

- Authorization: 필요 없음
- Accept: application/json
- GET 요청 본문: 없음

### 오류 처리 (Error Handling)

```json
{
  "success": false,
  "code": "COMMON_503_DATABASE_UNAVAILABLE",
  "message": "데이터베이스에 일시적으로 접근할 수 없습니다. 잠시 후 다시 시도해 주세요.",
  "data": null
}
```

검색 결과가 없는 경우에는 404가 아닌 200과 data: []를 반환한다.

## 전체 노드 조회 API

### 요청 파라미터 (Request Parameters)

없음. 전체 노드를 한 번에 반환한다.

### 응답 (Response)

- 200 OK: 전체 노드 조회 성공
- 503 Service Unavailable: 데이터베이스 접근 실패
- 500 Internal Server Error: 기타 서버 오류

요청 URL:

```http
GET /api/v1/nodes
```

응답 예시 — data는 실제 전체 목록 중 한 항목만 발췌:

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "nodeId": "EJ1_02_E",
      "building": "은주1관",
      "floorLabel": "2",
      "title": "은주1관 2층 엘베앞",
      "originalFilename": "은주1관_2층_엘베앞.jpg"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| nodeId | String | 출발·도착 선택에 사용할 노드 ID |
| building | String | 건물명 또는 야외 |
| floorLabel | String 또는 null | 원본 층 표기 |
| title | String | 파일명 기반 표시 이름 |
| originalFilename | String | 원본 사진 파일명 |

노드 ID 오름차순이며 페이징은 없다. 현재 데이터 기준 328개다. 노드가 없으면 data는 빈 배열이다. 간선·장소·imageUrl은 포함하지 않는다. 사진은 선택 후 경로 조회 API가 발급한 URL로 표시한다. 이 두 조회 API는 S3를 호출하지 않으며 URL을 발급하지 않는다.

### 헤더 정보 (Headers)

- Authorization: 필요 없음
- Accept: application/json
- GET 요청 본문: 없음

오류 형식과 공통 오류 코드는 장소 검색과 동일하다.

## 프론트 연결 순서

1. 사용자가 입력한 문자열로 GET /api/v1/places?keyword=검색어 호출
2. 검색 결과에서 출발·도착 장소 선택
3. 출발 장소의 arriveNodeId → fromNodeId, 도착 장소의 arriveNodeId → toNodeId
4. POST /api/v1/routes에 두 ID와 option 전달
5. 반환된 노드·장소·사진 URL과 통합 이동 안내 표시

자연어·음성으로 찾을 때는 1·2 대신 아래 [AI 장소 검색 API](#ai-장소-검색-api)를 사용한다. 후보의 `nodeId`를 그대로 fromNodeId·toNodeId로 쓴다. NFC 태그 등으로 노드 ID를 이미 알고 있으면 출발지 검색 없이 그 값을 fromNodeId로 바로 사용한다.

전체 사진 노드를 직접 선택하는 화면은 GET /api/v1/nodes의 nodeId를 사용한다. 프론트가 장소와 노드 매핑을 별도로 하드코딩할 필요는 없다.

## 캠퍼스 전체 둘러보기 API

### 요청 파라미터 (Request Parameters)

없음. 출발·도착·이동 옵션 없이 전체 그래프를 반환한다.

### 응답 (Response)

- 200 OK: 전체 캠퍼스 그래프 조회 성공
- 500 Internal Server Error: 잘못된 그래프 참조 또는 기타 서버 오류
- 503 Service Unavailable: DB 접근 또는 사진 URL 발급 실패

요청 URL:

```http
GET /api/v1/campus-map
```

응답 예시 — nodes·edges는 일부 항목만 발췌했으며 total 값은 현재 전체 DB 기준이다. imageUrl은 설명용 자리표시자이다.

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": {
    "nodes": [
      {
        "nodeId": "EJ1_02_E",
        "building": "은주1관",
        "floorLabel": "2",
        "title": "은주1관 2층 엘베앞",
        "originalFilename": "은주1관_2층_엘베앞.jpg",
        "imageUrl": "<은주1관 2층 사진의 Presigned URL>",
        "places": [
          {
            "placeId": "r-eunju1-202",
            "name": "도시공학과 실습실 (202호)",
            "kind": "room",
            "building": "은주1관",
            "floorLabel": "2",
            "roomNo": "202",
            "arriveNodeId": "EJ1_02_E",
            "arriveNodeTitle": "은주1관 2층 엘리베이터 앞",
            "doorPhoto": "door/IMG_5807.jpg",
            "aliases": "도시공학과 실습실 | 202호 | 은주1관 202",
            "sub": "은주1관 2층 202호"
          }
        ]
      }
    ],
    "edges": [
      {
        "fromNodeId": "EJ1_02_E",
        "toNodeId": "EJ1_01_E",
        "moveType": "ELEVATOR",
        "description": "엘리베이터를 타고 1층으로 내려갑니다."
      }
    ],
    "totalNodes": 328,
    "totalEdges": 918,
    "totalPlaces": 970
  }
}
```

| Field | Type | Description |
|---|---|---|
| data.nodes | MapNode[] | 고립 노드까지 포함한 전체 사진 노드. nodeId 오름차순 |
| data.edges | MapEdge[] | 원본 방향 간선. fromNodeId·toNodeId·moveType 순서 |
| data.totalNodes | Integer | 반환한 전체 노드 개수 |
| data.totalEdges | Integer | 반환한 전체 방향 간선 개수 |
| data.totalPlaces | Integer | nodes[].places에 포함된 장소 항목 수 |

MapNode 필드는 nodeId, building, floorLabel, title, originalFilename, imageUrl, places이다. places는 장소 검색 API와 같은 11개 필드이며 이름·ID 순서로 정렬한다. 연결된 장소가 없으면 빈 배열이다. 이웃 선택 전에는 이동 안내를 하나로 결정할 수 없으므로 노드 자체에는 description이 없다.

MapEdge 필드는 fromNodeId, toNodeId, moveType, description이다. 계단·엘리베이터 등 같은 노드 쌍의 다른 이동수단은 별도 간선으로 유지한다. 원본 이동 유형이 비어 있으면 moveType은 null이다. 간선별 description을 그대로 제공하며 길찾기 API와 달리 연속 엘리베이터를 합치지 않는다. 존재하지 않는 역방향 연결을 생성하지 않는다. 응답에서 참조하는 모든 노드는 nodes에 포함된다.

실제 사진 원본을 다운로드하지 않고 URL만 발급한다. imageUrl은 기본 1시간이며 사진 기능 비활성화 시 null이다. 만료되면 이 API를 다시 조회한다. 캐시가 없으면 전체 노드·간선·장소를 동일 DB 스냅샷에서 읽는다. 캐시가 있으면 DB 조회 없이 해당 데이터를 사용하고 사진 URL은 요청마다 새로 발급한다. 전체 URL 발급 중 실패하면 부분 결과 대신 오류를 반환한다.

### 프론트 둘러보기 흐름

1. GET /api/v1/campus-map으로 전체 데이터를 조회한다.
2. nodes를 nodeId 기준으로 보관하고 시작 사진의 imageUrl을 뷰어에 표시한다.
3. edges 중 fromNodeId가 현재 nodeId인 항목을 이동 선택지로 표시한다.
4. 사용자가 선택한 간선의 toNodeId에 해당하는 사진으로 이동한다.
5. 간선의 description으로 안내하고 해당 노드의 places를 표시한다.

간선 목록에 없는 이동은 허용하지 않는다. 양방향 이동은 각각의 방향 간선이 있는 경우에만 가능하다. UI에서 모든 사진을 한꺼번에 내려받을 필요는 없으며 현재 사진과 필요한 이웃 사진만 읽으면 된다.

이 데이터는 360도 사진을 따라 둘러보는 연결 그래프다. 현재 DB에는 지도 좌표·위경도·화살표 방향 각도가 없어 이 API에도 해당 정보는 포함하지 않는다. 실제 지도 위 노드 배치나 사진 위 정확한 화살표 위치에는 별도 데이터가 필요하다.

### 헤더 정보 (Headers)

- Authorization: 필요 없음
- Accept: application/json
- GET 요청 본문: 없음

### 오류 처리 (Error Handling)

| HTTP | Code | Description |
|---|---|---|
| 500 | GRAPH_500_NODE_REFERENCE | 잘못된 노드 연결 참조 |
| 503 | COMMON_503_DATABASE_UNAVAILABLE | DB 접근 실패 |
| 503 | PHOTO_503_SIGNING_FAILED | 사진 URL 발급 실패 |
| 500 | COMMON_500 | 기타 서버 오류 |

오류 응답은 공통 형식이며 data는 null이다.

## AI 장소 검색 API

"북악관 102호", "프린트 하고 싶어", "나 지금 카페 앞이야"처럼 자연스러운 문장이나 녹음으로 출발지·목적지 후보를 찾는다. AI는 장소·노드 후보만 고르며 경로는 만들지 않는다. 후보의 `nodeId`를 경로 조회 API에 그대로 넘긴다.

| 순서 | API | 설명 |
|---|---|---|
| 1 | `POST /api/v1/searches` (JSON) | 텍스트 검색 |
| 1 | `POST /api/v1/searches` (multipart) | 음성 검색. 같은 경로이며 Content-Type으로 구분 |
| 2 | `PUT /api/v1/searches/{searchId}/selection` | 사용자가 고른 후보 기록 |
| 3 | `POST /api/v1/routes` | 후보의 nodeId로 경로 조회 |

### 텍스트 검색

```http
POST /api/v1/searches
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `target` | string | O | `ORIGIN`(지금 있는 곳, 출발지) 또는 `DESTINATION`(가고 싶은 곳, 목적지) |
| `query` | string | O | 사용자 문장. 앞뒤 공백 제거 후 1~200자 |
| `limit` | integer | X | 후보 최대 개수 1~10. 생략 시 5 |

```json
{
  "target": "ORIGIN",
  "query": "나 지금 카페 앞이야",
  "limit": 5
}
```

### 음성 검색

```http
POST /api/v1/searches
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `target` | string | O | `ORIGIN` 또는 `DESTINATION` |
| `audio` | file | O | 녹음 파일(m4a·wav·webm 등). 1MB 이하 |
| `durationMs` | integer | X | 녹음 길이(ms). 15000 이하. STT 대기 시간 계산에 사용 |
| `limit` | integer | X | 후보 최대 개수 1~10. 생략 시 5 |

- 음성은 Oracle STT 서버(faster-whisper)로 먼저 글자로 바꾸고, 서버가 응답하지 않거나 실패하면 OpenAI `gpt-4o-mini-transcribe`로 대체한다.
- 인식한 문장은 응답 `transcript`, 사용한 엔진은 `sttEngine`(`WHISPER` 또는 `OPENAI`)에 담긴다.
- 이후 처리는 텍스트 검색과 같다. 보통 3~6초 걸린다.

### 검색 응답

HTTP `201 Created`, `Location: /api/v1/searches/{searchId}`. 후보가 없어도 오류가 아니며 `status`가 `NOT_FOUND`, `candidates`가 빈 배열이다.

```json
{
  "success": true,
  "code": "201",
  "message": "검색 후보를 만들었습니다.",
  "data": {
    "searchId": 12,
    "target": "ORIGIN",
    "inputType": "TEXT",
    "transcript": null,
    "sttEngine": null,
    "status": "CANDIDATES",
    "message": "어디에 계신지 골라 주세요.",
    "routeOption": null,
    "candidates": [
      {
        "candidateId": "place:r-yudam-907",
        "type": "PLACE",
        "placeId": "r-yudam-907",
        "nodeId": "s1013_105937",
        "name": "로렐 카페 (907호)",
        "displaySub": "유담관 9층 907호",
        "building": "유담관",
        "floorLabel": "9",
        "kind": "facility",
        "reason": "근처 시설: 카페",
        "score": 0.93,
        "matchedBy": "BOTH"
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `searchId` | integer | 검색 ID. 후보 선택 API 경로에 사용 |
| `target` | string | 요청한 검색 대상 |
| `inputType` | string | `TEXT` 또는 `VOICE` |
| `transcript` | string 또는 null | 음성 인식 결과. 텍스트 검색이면 null. "이렇게 들었어요" 표시용 |
| `sttEngine` | string 또는 null | `WHISPER` 또는 `OPENAI`. 텍스트 검색이면 null |
| `status` | string | `CANDIDATES` 또는 `NOT_FOUND` |
| `message` | string | 화면에 그대로 보여 줄 안내 문구 |
| `routeOption` | string 또는 null | 문장에서 찾은 경로 옵션. 목적지 검색에서만. 경로 조회 API의 option으로 전달 |
| `candidates` | Candidate[] | 후보 목록. 점수 높은 순 |

#### Candidate

| 필드 | 타입 | 설명 |
|---|---|---|
| `candidateId` | string | `place:<placeId>` 또는 `node:<nodeId>`. 후보 선택 API에 그대로 보냄 |
| `type` | string | `PLACE`(장소) 또는 `NODE`(사진 지점. 출발지 검색에서만) |
| `placeId` | string 또는 null | 장소 ID. NODE면 null |
| `nodeId` | string | **경로 조회 API의 fromNodeId·toNodeId로 사용할 노드 ID** |
| `name` | string | 표시 이름 |
| `displaySub` | string 또는 null | 보조 설명(건물·층·호수) |
| `building` | string 또는 null | 건물명 |
| `floorLabel` | string 또는 null | `1`, `B1`, `L` 등 층 표기 |
| `kind` | string 또는 null | room, professor, facility, office, building |
| `reason` | string 또는 null | 업무·주변 시설로 찾았을 때 그 이유. 출발지는 `근처 시설: `, 목적지는 `업무: `로 시작 |
| `score` | number | 일치 점수 0~1 |
| `matchedBy` | string | `KEYWORD`(글자 일치), `SEMANTIC`(의미 유사), `BOTH` |

#### 문장 처리 규칙

| 규칙 | 예 |
|---|---|
| 말끝 제거 | "북악관 7층 엘리베이터 앞이야" → "북악관 7층 엘리베이터 앞" |
| 군더더기 제거 | "나 지금 카페 앞이야" → "카페 앞" |
| 위치 단어 분리 | "카페 앞" → 핵심어 "카페"도 함께 검색 |
| 층·호수 붙이기 | "지하 1 층" → "지하1층", "102 호" → "102호" |
| 경로 옵션 추출 (목적지만) | "계단 말고·엘리베이터로·편하게" → `COMFORTABLE`, "실내로·지하로" → `INDOOR_UNDERGROUND_FIRST`, "제일 빠른·최단·빨리" → `SHORTEST` |

- 출발지(`ORIGIN`)는 장소·사진 지점·업무 문서에서, 목적지(`DESTINATION`)는 장소·업무 문서에서 찾는다.
- 업무("프린트", "동아리방" 등)로 찾으면 그 업무에 연결된 장소들이 후보가 된다. 출발지는 같은 노드 후보를 하나로 합친다.
- 점수는 글자 유사도(pg_trgm)와 의미 유사도(OpenAI 임베딩)를 합친 값이며 0.45 미만은 제외한다. OpenAI 키가 없거나 임베딩 호출이 실패하면 글자 점수만으로 검색하며 검색 자체는 실패하지 않는다.

### 후보 선택

```http
PUT /api/v1/searches/{searchId}/selection
Content-Type: application/json
```

```json
{
  "candidateId": "place:r-yudam-907"
}
```

HTTP `200 OK`, `data`는 null이다. 검색 품질 분석용 기록이며 경로 조회 전에 호출한다. 같은 검색에서 다시 고르면 마지막 선택으로 덮어쓴다.

### 오류

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `COMMON_400_VALIDATION` | target 누락, limit 범위 밖, candidateId 공백 |
| 400 | `COMMON_400_BODY_NOT_READABLE` | JSON 오류, 정의되지 않은 target |
| 400 | `SEARCH_400_INVALID_QUERY` | query가 비었거나 200자 초과 |
| 400 | `SEARCH_400_NOT_A_CANDIDATE` | 그 검색의 후보에 없는 candidateId |
| 404 | `SEARCH_404_NOT_FOUND` | 없는 searchId |
| 400 | `STT_400_INVALID_AUDIO` | audio 누락·빈 파일·읽을 수 없는 형식, durationMs가 0 이하이거나 15000 초과 |
| 413 | `STT_413_AUDIO_TOO_LARGE` | audio 1MB 초과 |
| 422 | `STT_422_EMPTY_TRANSCRIPT` | 말소리가 없거나 인식 결과가 빔 |
| 503 | `STT_503_UNAVAILABLE` | Oracle STT와 OpenAI 대체가 모두 실패 |

STT 오류 message는 사용자에게 그대로 보여 줄 수 있는 문장이다. 예: "잘 듣지 못했어요. 조금 더 크게 말해 주세요."

### 프론트 AI 검색 흐름

1. 출발지·목적지 입력칸에서 글자 입력 또는 녹음 → `POST /api/v1/searches` (`target` 구분)
2. `status`가 `NOT_FOUND`면 `message`를 보여 주고 다시 입력받는다.
3. 후보 목록에서 사용자가 고르면 `PUT /searches/{searchId}/selection`
4. 출발·도착 후보의 `nodeId`와 목적지 응답의 `routeOption`으로 `POST /api/v1/routes`
5. NFC 태그로 출발 노드를 이미 알면 출발지 검색은 생략한다.

### 검색 문서 갱신 (관리자)

```http
POST /api/v1/admin/embedding-jobs?target=ALL&force=false
X-Admin-Key: <ADMIN_API_KEY>
```

장소·노드·업무 데이터로 검색 문서(`search_documents`)를 다시 만들고, 내용이 바뀐 문서만 임베딩한다. **배포 후 처음 한 번은 반드시 실행**한다. 실행 전에는 문서가 없어 모든 검색이 `NOT_FOUND`다. 장소·업무 데이터를 바꾼 뒤에도 다시 실행한다. FE 연동 대상이 아니다.

| 파라미터 | 설명 |
|---|---|
| `target` | `ALL`(기본), `SERVICE`, `PLACE`, `NODE` |
| `force` | true면 바뀌지 않은 문서도 전부 다시 임베딩. 기본 false |

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": {
    "documentCount": 1326,
    "serviceUpdated": 28,
    "placeUpdated": 970,
    "nodeUpdated": 328,
    "model": "text-embedding-3-small (1024차원)",
    "elapsedMs": 41250
  }
}
```

- `X-Admin-Key`가 없거나 다르면 401 `ADMIN_401_UNAUTHORIZED`. 서버에 `ADMIN_API_KEY`가 비어 있으면 항상 401이다.
- OpenAI 키가 없으면 문서만 만들고 임베딩은 건너뛴다(`model`이 null). 이 경우 검색은 글자 점수로만 동작한다.

### 관련 설정

| 환경 변수 | 설명 |
|---|---|
| `OPENAI_API_KEY` | 질문·문서 임베딩, STT 대체에 사용. 없으면 글자 검색만 |
| `STT_BASE_URL` | Oracle STT 서버 주소. 기본 `https://stt.eunhasu03.cloud` |
| `STT_TOKEN` | Oracle STT 서버 토큰(`X-STT-Token`). 없으면 바로 OpenAI로 대체 |
| `STT_HEALTH_CHECK_ENABLED` | Oracle STT 30초 주기 상태 확인. 기본 true |
| `ADMIN_API_KEY` | 검색 문서 갱신 API 키 |

DB는 `vector`(pgvector)·`pg_trgm` 확장이 필요하다. `db/init/07-ai-search-schema.sql`이 확장과 `services`, `service_places`, `search_documents`, `search_log` 테이블을 만들고, `09-services-seed.sql`이 업무 28개·업무-장소 연결 85개를 넣는다.

## 전체 목록 Redis 캐시

응답 JSON 구조는 동일하며 아래 세 API에 DB 데이터 캐시를 적용한다.

| API | Redis 키 | 재조회 동작 |
|---|---|---|
| GET /api/v1/campus-map | smap:campus:v1:map | 전체 지도 데이터를 재사용하고 사진 URL만 새로 발급 |
| GET /api/v1/nodes | smap:campus:v1:nodes | 전체 노드 DTO 목록을 재사용 |
| GET /api/v1/places | smap:campus:v1:places | 검색어가 없거나 공백일 때 전체 연결 장소 목록을 재사용 |

- 기본 TTL은 300초다. 최초 요청·만료 후 요청은 DB에서 생성하며, 이후 요청은 만료 전까지 Redis에서 조회한다.
- keyword가 있는 장소 검색과 POST /api/v1/routes는 이번 캐시 대상이 아니다.
- Redis에는 JSON 형태의 노드·간선·장소 데이터만 저장한다. Presigned URL·AWS 자격 증명은 저장하지 않는다.
- Redis 읽기·쓰기 장애 시 DB 결과로 대체하며 잘못된 캐시 JSON도 다시 생성한다. 캐시가 꺼져 있으면 Redis를 사용하지 않는다.
- 캐시 적중 시 데이터 로더와 DB 트랜잭션을 시작하지 않는다.
- DB를 직접 수정하면 전체 목록은 최대 TTL만큼 이전 데이터가 보일 수 있다. 세 키를 지우면 다음 요청에서 즉시 다시 만든다.
- S3 원본 사진 다운로드 속도나 전체 JSON의 전송 크기를 줄이는 기능은 아니다.

설정:

```dotenv
CAMPUS_CACHE_ENABLED=true
CAMPUS_CACHE_TTL_SECONDS=300
REDIS_HOST=localhost
REDIS_PORT=6380
```

로컬 개발은 다른 프로젝트의 6379 포트와 충돌하지 않도록 SMAP Redis를 6380에 연결한다. Redis 컨테이너 내부와 운영 앱 컨테이너 간 접속 포트는 기존 6379다. 로컬 서버는 .env 변경 후 재시작해야 한다.

로컬 SMAP 캐시만 즉시 비우기. 다른 프로젝트의 Redis와 다른 키는 삭제하지 않는다. 비밀번호를 설정했다면 아래 redis-cli 뒤에 `--askpass`를 추가하여 프롬프트에 입력한다.

```powershell
docker compose --env-file .env -f docker-compose-dev.yaml exec redis redis-cli DEL smap:campus:v1:map smap:campus:v1:nodes smap:campus:v1:places
```
