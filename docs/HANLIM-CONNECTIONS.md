# 한림관 연결 정리 — V5

사용자 확정에 따라 HL03_P1 노드와 관련 간선을 제거하고 새 연결을 양방향으로 확보한다.
사진 객체는 삭제하지 않는다. HL03_P1의 NODE 검색 문서도 제거하여 존재하지 않는 노드가 검색되지 않게 한다.
search_documents 테이블이 없는 환경에서는 검색 문서 정리만 건너뛰고 노드·간선 변경은 진행한다.
V5가 검색 테이블을 새로 생성하지는 않는다.

## 삭제

- HL03_P1 ↔ HL03_DINING_STAIRS_FRONT
- HL03_P1 ↔ HL03_EUNJU_LINK
- HL03_P1 ↔ HL03_LOUNGE
- HL03_HALL ↔ HL03_MAIN_PATH
- HL03_P1 노드 1개와 해당 NODE 검색 문서 1개

총 방향 간선 8개를 삭제한다. 장소 참조가 있거나 P1에 다른 연결이 추가되어 있으면 마이그레이션은 중단한다.

## 추가

아래 방향은 사용자 안내문·각도를 적용하며, 반대 방향은 WALK·안내문 NULL·각도 NULL로 추가한다.

| 출발 | 도착 | 유형 | 정방향 각도 |
| --- | --- | --- | ---: |
| HL03_DINING_STAIRS_FRONT | HL03_EUNJU_LINK | WALK | 107 |
| HL03_DINING_STAIRS_FRONT | HL03_MAIN_PATH | WALK | 261 |
| HL03_EUNJU_LINK | HL03_HALL | WALK | 243 |
| HL03_HALL | HL03_LOUNGE | WALK | 261 |
| HL03_EUNJU_STAIRS | EJ1_01_END_STAIRS | WALK | 94 |
| HL03_MAIN_LINK | s1014_101449 | WALK | 332 |

은주1관 연결 대상은 최종 정정한 1층이다. 초기 행에 포함됐던 2층·138도는 이 추가 간선에 사용하지 않는다.
기존 HL03_EUNJU_STAIRS ↔ EJ1_02_END_STAIRS 연결과 기존 역방향 193도는 그대로 보존한다.
건물·층·사진명은 nodes에서 가져와 3층·1층·지상 표기를 유지한다.

## 운영 검증

- 노드 328 → 327개
- 간선 918 → 922개: 삭제 8개·추가 12개
- 각도 있음 916개, 없음 6개
- 나머지 기존 간선의 전체 필드 변경 0개
- 모든 남은 노드는 스콘스퀘어에서 도달할 수 있고 돌아올 수 있다.
- 노드·간선·검색 문서는 선적용 전 build/hanlim-update/before.sql로 백업했다.

운영 선적용 이후 Flyway가 V5를 재실행해도 중복 추가·역방향 값 초기화가 발생하지 않는다.
적용 후 SMAP 캠퍼스 메타데이터 캐시를 무효화한다. 이미 열린 클라이언트는 목록을 다시 조회해야 한다.

## V6 — 역방향 안내문·각도 확정

V5에서 일시적으로 NULL로 두었던 역방향 6개는 V6에서 사용자 확정 안내문·각도로 갱신한다.
V5 파일은 수정하지 않으며 V6는 기존 WALK 간선의 description·arrow_yaw_deg만 UPDATE한다.

| 출발 | 도착 | 확정 각도 |
| --- | --- | ---: |
| HL03_EUNJU_LINK | HL03_DINING_STAIRS_FRONT | 260 |
| HL03_MAIN_PATH | HL03_DINING_STAIRS_FRONT | 315 |
| HL03_HALL | HL03_EUNJU_LINK | 63 |
| HL03_LOUNGE | HL03_HALL | 53 |
| EJ1_01_END_STAIRS | HL03_EUNJU_STAIRS | 257 |
| s1014_101449 | HL03_MAIN_LINK | 323 |

V6 운영 반영 후 노드 327개·간선 922개를 유지하며 각도가 NULL인 간선은 0개다.
대상 외의 간선은 변경하지 않았고, 기존 정방향 94도 및 2층 역방향 193도도 유지했다.
적용 전 간선 백업은 build/hanlim-reverse-update/edges-before.sql에 보관했다.
