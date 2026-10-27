# DB 재구성 상태

새 연결 CSV에서 추출한 노드 328개를 DB에 적재했다. 이전 데이터는 백업했다.

## 현재 유지한 파일

- init/01-init.sql: PostgreSQL 확장 초기화.
- init/02-campus-schema.sql: 새 노드 4개 컬럼 및 현재 간선·장소 테이블 구조.
- init/03-nodes-seed.sql: data/smap_nodes.csv에서 생성한 328개 노드의 재실행 가능한 시드.
- init/04-edges-seed.sql: 연결 CSV의 918개 방향 간선과 안내 문구를 적재한다.
- init/05-places-seed.sql: 보관본에서 제외·보류 범위와 북악관 ID 변경을 반영한 장소 969개.

이전 데이터 시드와 연결 수정 SQL 03~06은 보관 폴더로 옮겼다.
빈 DB로 초기화하면 새 노드·간선·장소를 순서대로 자동 적재한다. 새 03-nodes-seed.sql은 기존 03-campus-seed.sql과 다른 파일이다.

## 현재 테이블

- nodes: 328행. node_id, building, floor_label, original_filename.
- edges: 918행. CSV와 동일한 10개 컬럼: from_node_id, from_building, from_floor, from_photo, to_node_id, to_building, to_floor, to_photo, move_type, description.
- places: 969행. 보관본의 12개 컬럼을 유지하며 arrive_node_id 외래키로 nodes와 연결한다.

새 노드를 적재하기 전 기존 노드 335개·간선 874개·장소 1,012개를 초기화했다.
예제 테이블과 Redis는 초기화하지 않았다. 노드 외의 이전 컬럼 10개는 제거했다.
경로 API는 새 간선을 사용하고 구간별 description을 그대로 반환한다.
사용자가 시간 컬럼을 없앴으므로 DB·엔티티·API에서 시간을 제거했다. 추정 시간은 넣지 않는다.
352행의 빈 move_type도 보완하지 않고 빈 문자열로 저장한다. API는 해당 이동 유형을 NULL로 표현한다.
모든 간선의 가중치는 1이며 다익스트라로 이동 구간 수를 최소화한다. API에는 totalSteps를 반환한다. 예상 시간 필드는 없다.
외부경로·엘리베이터·지하도보 우선 조건은 현재 적용하지 않으며 요청에는 출발·도착 노드 ID만 받는다.
이전 조건 구현과 관련 테스트는 archive/deferred-route-preferences에 보관했다.
연결 방향을 그대로 보존하며 역방향을 자동 생성하지 않았다. s1014_101449는 원본 방향상 버스정류장에서 도달할 수 없으므로 임의 보완하지 않았다.

전체 DB 백업: archive/db-before-node-reset-2026-10-27/smap-001949128.dump.
pg_restore --list로 읽기 가능 여부 및 3개 캠퍼스 테이블의 백업 포함을 검증했다.

## 보관 위치

`../archive/legacy-campus-2026-10-27/`에 이전 CSV·검수 자료·시드 SQL·도구를 보관했다.
원래 경로와 체크섬은 보관 폴더의 manifest.json을 참고한다.

루트의 smap-연결노드.csv에서 노드 328개를 추출한 결과는 data/smap_nodes.csv에 있다.
사진 충돌은 사용자 지시에 따라 다수 사진으로 통일했으며 DB와 원본 필드가 전부 일치한다.

## 장소 매핑

보관본 1,012개 장소를 db/data/smap_places.csv에 복원했다. 원본은 수정하지 않는다.
대일관 6·7층 18개는 제외하고 은주1관 47개·은주2관 23개는 보류했다.
나머지 969개를 data/smap_places.mapped.csv로 만들고 DB에 적재했다.
북악관 18개 장소의 bukak_1f_elev 참조만 사용자 확정 새 ID bukak_7f_elev로 변경했다.
대상 장소의 미매핑·존재하지 않는 노드 참조는 0개이며, 노드·간선은 변경하지 않았다.
노드별 장소는 PlaceRepository.findByArriveNodeIdOrderByNameAsc로 조회한다.

장소와 노드의 건물·층이 다른 기존 매핑 17개는 원본대로 유지했다. 문예관 3층과 L층은 확정 규칙상 같은 층으로 취급하여 제외했다.
실제 도착 지점의 정확성을 보장하는 새 현장 검증은 아니며 data/place-mapping-review.csv에서 별도 검수한다.

은주1관의 기존 노드 매핑 45개를 추가 적재했다. 은주2관은 삭제한 21개와 도착 노드가 빈 기숙사 입구 2개 외에 적재 가능한 장소가 없다. 노드와 간선은 변경하지 않았다.
