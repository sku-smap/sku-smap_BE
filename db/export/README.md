# RDS 데이터 이관

`smap-campus-rds.sql`은 2026-10-27 현재 로컬 PostgreSQL의 캠퍼스 테이블을 직접 추출한 SQL이다.

- nodes: 328개
- edges: 918개
- places: 969개

테이블, 기본키, 외래키, 인덱스와 컬럼명을 명시한 INSERT문을 포함한다. 소유자·권한·DB 생성·테이블 삭제 명령은 없다. example 테이블, 확장, Redis와 이미지는 포함하지 않는다. 경로 가중치 1은 애플리케이션에서 적용하므로 DB에 시간이나 가중치 컬럼을 추가하지 않는다.

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
