# 층 표기와 Flyway 전환

노드 `floorLabel`은 숫자·L·B 표기에 `층`을 붙인다. 예: `1층`, `L층`, `B1층`.
사용자가 확인한 기존 NULL 노드 43개는 `지상` 41개, `지하` 1개,
`연결계단` 1개로 변경한다. 연결계단은 지상·지하 판정값이 아니다.
층별 지도 좌표는 이 변경에 포함하지 않는다.

`V1__normalize_floor_labels.sql`은 기존 노드·간선·장소를 UPDATE한다.
간선의 출발·도착 층은 노드와 맞추고, 장소는 도착 노드의 층을 복사하지 않고
기존 장소 층의 표기만 변경한다. ID·사진명·연결 관계는 유지한다.
등록되지 않은 노드 ID 또는 목록에 없는 NULL 노드가 있으면 전체 마이그레이션이 실패한다.

기존 스키마는 Flyway baseline 0으로 등록한 뒤 V1을 적용한다.
최초 전환 시 기존 `db/init` SQL 실행 후 앱 시작 시 Flyway가 실행된다.
이후 배포에서는 `flyway_schema_history`가 존재하면 기존 init SQL을 재실행하지 않는다.
앞으로의 데이터·스키마 변경은 `src/main/resources/db/migration/V2__...sql` 이후 버전으로 작성한다.
적용된 마이그레이션 파일은 수정하지 않는다.

기존 Redis 데이터가 변경 전 층을 반환하지 않도록 캠퍼스 캐시 키를 v2로 올렸다.
검색 문서와 임베딩은 기존 관리자 갱신 API를 실행해야 최신 층 정보가 반영된다.

운영 배포 후 확인:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT floor_label, count(*) FROM nodes GROUP BY floor_label ORDER BY floor_label;
SELECT count(*) FROM nodes WHERE floor_label IS NULL;
SELECT count(*) FROM edges e JOIN nodes n ON n.node_id = e.from_node_id
WHERE e.from_floor IS DISTINCT FROM n.floor_label;
SELECT count(*) FROM edges e JOIN nodes n ON n.node_id = e.to_node_id
WHERE e.to_floor IS DISTINCT FROM n.floor_label;
```

NULL 층 및 간선 층 불일치 건수는 각각 0이어야 한다.
운영 DB에는 배포 전 백업을 보관하고, 로컬 수정만으로 운영 데이터가 변경되지는 않는다.
