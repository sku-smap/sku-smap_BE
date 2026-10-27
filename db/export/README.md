# RDS 데이터 이관

`smap-campus-rds.sql`은 2026-10-27 현재 로컬 PostgreSQL의 캠퍼스 테이블을 직접 추출한 SQL이다.

- nodes: 328개
- edges: 918개
- places: 969개

테이블, 기본키, 외래키, 인덱스와 컬럼명을 명시한 INSERT문을 포함한다. 소유자·권한·DB 생성·테이블 삭제 명령은 없다. example 테이블, 확장, Redis와 이미지는 포함하지 않는다.
탐색 비용은 RouteWeightPolicy에서 요청 option에 따라 동적으로 계산한다. SHORTEST는 모든 간선 1, 기본 OUTDOOR_FIRST는 실내·지하 끝점마다 +1, INDOOR_UNDERGROUND_FIRST는 야외 끝점마다 +1이다. COMFORTABLE은 계단 +5와 야외·지하 끝점마다 +1을 기본 1에 더한다. 지하는 편한길에서 야외로 취급한다.
DB에 시간·가중치 컬럼은 없으며 totalSteps는 비용 합이 아닌 통합 전 간선 수다. 요청의 option 생략·null은 OUTDOOR_FIRST이다.

## 실행

RDS PostgreSQL의 대상 DB를 준비하고, nodes·edges·places 테이블이 없는 상태에서 실행한다. 이미 테이블이 존재하면 실패하며 기존 데이터를 자동 삭제하지 않는다. 전체 SQL은 하나의 트랜잭션으로 실행한다.

저장소 루트에서 PostgreSQL 클라이언트로 실행한다. 실제 접속 주소·계정·DB 이름을 입력하고 비밀번호는 프롬프트에서 입력한다.

```powershell
psql "host=YOUR_RDS_ENDPOINT port=5432 dbname=smap user=YOUR_DB_USER sslmode=require" -W -X -v ON_ERROR_STOP=1 -f "db/export/smap-campus-rds.sql"
```

DBeaver 등 SQL 편집기에서도 UTF-8로 파일을 열고 전체 스크립트를 실행할 수 있다. 파일에는 psql 전용 메타 명령이 없다.

## 확인

```sql
SELECT 'nodes' AS table_name, count(*) FROM public.nodes
UNION ALL SELECT 'edges', count(*) FROM public.edges
UNION ALL SELECT 'places', count(*) FROM public.places;

SELECT count(*) AS invalid_place_nodes
FROM public.places p
LEFT JOIN public.nodes n ON n.node_id = p.arrive_node_id
WHERE n.node_id IS NULL;
```

별도 임시 PostgreSQL DB에서 전체 복원 성공과 위 행 수, 잘못된 장소 노드 참조 0개를 검증했다. 이 파일은 생성 시점의 스냅샷이므로 데이터가 변경되면 다시 추출해야 한다.
