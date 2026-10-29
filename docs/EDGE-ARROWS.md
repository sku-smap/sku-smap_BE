# 간선 화살표 각도

V4는 edges에 nullable double precision 컬럼 arrow_yaw_deg를 추가한다.
값은 CSV의 원본 각도이며 0 이상 360 미만으로 제한한다. 0도 기준 방향이나 좌표계는
이 마이그레이션에서 추정·변환하지 않는다. arrow_source는 적재하지 않는다.

## 매핑 기준

- from_node_id + to_node_id가 일치하는 기존 간선만 UPDATE한다.
- move_type은 매칭 조건에 포함하지 않는다. 같은 노드 쌍의 복수 이동 유형은 같은 각도를 사용한다.
- CSV에만 있는 연결은 INSERT하지 않으며 DB에만 있는 연결도 DELETE하지 않는다.
- 이동 유형·층·사진명·안내문은 변경하지 않는다.
- 동일 노드 쌍의 CSV 각도 충돌은 없었다.
- CSV에는 추정 각도도 포함되어 있으며, 이번 작업은 현장 방향 검증이 아니다.

## 운영 적용 결과

smapp.csv 917개 행을 운영 간선 918개와 비교했다.
운영에 존재하는 786개 노드 쌍에 대해 914개 간선의 각도를 입력했다.
CSV에만 있는 연결 5개는 제외했다. 이 중 각도와 출처가 한 셀에 섞인 2개도 적재 대상에서 제외되었다.

각도가 없는 기존 간선 4개는 NULL로 유지했다.

| 출발·도착 | 이동 유형 |
| --- | --- |
| HL03_P1 → HL03_LOUNGE | WALK |
| HL03_LOUNGE → HL03_P1 | WALK |
| HL03_HALL → HL03_MAIN_PATH | WALK |
| HL03_MAIN_PATH → HL03_HALL | WALK |

운영 간선 백업을 build/arrow-yaw-update/edges-before.sql에 보관했다.
적용 전후 arrow_yaw_deg를 제외한 전체 간선 데이터의 체크섬이 일치했다.
운영에는 단일 트랜잭션으로 선적용했으며, V4는 다음 Flyway 실행에서 안전하게 재적용·등록될 수 있다.
## API 응답

| API | 각도 위치 |
| --- | --- |
| POST /api/v1/routes | data.segments[].arrowYawDeg |
| GET /api/v1/nodes | data[].outgoingEdges[].arrowYawDeg |
| GET /api/v1/campus-map | data.edges[].arrowYawDeg |

arrowYawDeg는 항상 키가 존재하며 number 또는 null이다. null을 0도로 취급하지 않는다.
같은 노드에 이동 선택지가 여러 개이므로 노드 최상위에 단일 각도를 제공하지 않는다.
outgoingEdges는 fromNodeId, toNodeId, moveType, description, arrowYawDeg를 포함한다.
나가는 간선이 없으면 빈 배열이며 다른 노드의 간선이나 역방향을 임의로 생성하지 않는다.
노드와 간선은 같은 DB 스냅샷에서 읽는다. /nodes는 사진 URL을 발급하지 않는다.

경로의 연속 엘리베이터 구간이 합쳐지면 시작 사진 기준인 첫 원본 간선의 각도를 사용한다.
중간 구간의 각도를 평균하거나 첫 각도가 null일 때 뒤 구간 값으로 대체하지 않는다.
통합 segments의 toNodeId는 최종 하차 지점이며, 각도는 첫 실제 이동 방향을 뜻한다.
중간 사진의 원본 이동 선택지는 campus-map 또는 nodes의 outgoingEdges를 참조한다.

```json
{
  "fromNodeId": "field",
  "toNodeId": "hill_field_entrance",
  "moveType": "WALK",
  "description": "폭풍의언덕 스콘스퀘어 입구로 이동합니다.",
  "arrowYawDeg": 303.0
}
```

이번 API 변경은 코드 배포 후 적용된다. 운영 DB 적재와는 별도이다.
