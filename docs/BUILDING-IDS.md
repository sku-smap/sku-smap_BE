# 고정 건물 ID

`buildingId`는 식별용 고정 ID이고 기존 `building`은 표시용 이름이다.
표시명을 바꿀 때 기존 ID를 새로 생성하지 않는다. 노드 ID도 변경하지 않는다.

## 대응표 조회 API

`GET /api/v1/buildings` — 인증·요청 파라미터 없음.

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": [
    {"buildingId": "HYEIN", "building": "혜인관"},
    {"buildingId": "OUTDOOR_GROUND", "building": "야외"},
    {"buildingId": "OUTDOOR_UNDERGROUND", "building": "야외"}
  ]
}
```

위 응답은 일부 항목 예시다. 실제 nodes·places에 저장된 ID·표시명 쌍을 중복 제거하고
buildingId·building 순으로 정렬한다. 같은 표시명 `야외`여도 ID가 다르면 별도 항목이다.
데이터가 없으면 HTTP 200과 data: []를 반환한다.
DB에 없는 예약 ID는 반환하지 않는다. OUTDOOR_UNKNOWN 대상이 없으면 목록에도 없다.
Redis 캐시·사진 URL 발급 없이 최신 DB를 조회한다. V3 마이그레이션 적용이 필요하다.

| buildingId | building |
| --- | --- |
| BON | 본관 |
| BUKAK | 북악관 |
| CHUNGWOON | 청운관 |
| DAEIL | 대일관 |
| EUNJU1 | 은주1관 |
| EUNJU2 | 은주2관 |
| HYEIN | 혜인관 |
| HANLIM | 한림관 |
| MOONYE | 문예관 |
| SANGSEUNG | 상승관 |
| YUDAM | 유담관 |
| SUIN | 수인관 |
| MOONYE_BUKAK_CONNECTOR | 문예관-북악관 연결구간 |
| OUTDOOR_GROUND | 야외 — 지상 |
| OUTDOOR_UNDERGROUND | 야외 — 지하 |
| OUTDOOR_UNKNOWN | 야외 — 층으로 판정 불가 |

야외의 표시명은 지상·지하 모두 기존 `야외`를 유지한다.
야외 ID 분류는 `floorLabel`만 사용하며 사진명으로 추정하지 않는다.

- `지하`, `B1층`, `B2층`, `지하1층`, 음수층 표기: OUTDOOR_UNDERGROUND
- `지상`, `야외`, 양의 숫자층·0층, L층: OUTDOOR_GROUND
- 그 외 또는 층 없음: OUTDOOR_UNKNOWN
- 장소 층이 비어 있으면 도착 노드의 층을 참고한다. 장소의 building·floorLabel 값은 변경하지 않는다.
- 건물명이 있는 노드는 층이 `지상`이어도 해당 건물 ID를 사용한다.
- 건물·장소 ID가 도착 노드의 건물 ID와 다른 기존 매핑은 그대로 유지한다.

예시:

```json
{"buildingId":"HYEIN","building":"혜인관","floorLabel":"1층"}
```

```json
{"buildingId":"OUTDOOR_UNDERGROUND","building":"야외","floorLabel":"B1층"}
```

제공 위치는 `/nodes`의 data[], `/places`의 data[], `/campus-map` 및 `/routes`의
data.nodes[]와 하위 places[], AI 검색 응답의 candidates[]이다.
간선은 노드 ID로 건물 정보를 참조한다.
AI 검색은 원본 nodes·places에서 ID를 조회하므로 ID만 반영하기 위한 재임베딩은 필요하지 않다.

V3 마이그레이션은 기존 nodes·places에 building_id 컬럼을 추가하고 값을 채운다.
새 테이블은 생성하지 않는다. 노드·장소 원래 값과 간선은 변경하지 않는다.
새로운 건물명이 추가되면 추정 ID를 만들지 않고 마이그레이션이 실패한다.
향후 신규 노드·장소 INSERT에는 building_id를 함께 지정해야 한다.
일반 API에 쓰기 기능은 없으며 기존 init seed는 Flyway 이력 생성 이후 재실행하지 않는다.
