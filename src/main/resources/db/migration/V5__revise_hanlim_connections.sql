-- 사용자 확정: HL03_P1 미사용, 한림관 연결 4쌍 삭제 및 6쌍 양방향 확보.
-- 은주1관 연결 대상은 사용자 최종 정정에 따라 EJ1_01_END_STAIRS 1층이다.
-- 운영 선적용 이후에도 안전하게 적용 이력을 등록할 수 있도록 재실행 가능하게 작성한다.
LOCK TABLE nodes, edges, places IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE hanlim_new_connections (
    from_node_id text NOT NULL,
    to_node_id text NOT NULL,
    description text NOT NULL,
    arrow_yaw_deg double precision NOT NULL,
    PRIMARY KEY (from_node_id, to_node_id)
) ON COMMIT DROP;

INSERT INTO hanlim_new_connections VALUES
    ('HL03_DINING_STAIRS_FRONT', 'HL03_EUNJU_LINK', '통로를 따라 한림관 3층 은주1관연결 통로로 이동합니다.', 107),
    ('HL03_DINING_STAIRS_FRONT', 'HL03_MAIN_PATH', '통로를 따라 한림관 3층 본관연결길로 이동합니다.', 261),
    ('HL03_EUNJU_LINK', 'HL03_HALL', '통로를 따라 한림관 3층 복도로 이동합니다.', 243),
    ('HL03_HALL', 'HL03_LOUNGE', '통로를 따라 한림관 3층 스콘라운지로 이동합니다.', 261),
    ('HL03_EUNJU_STAIRS', 'EJ1_01_END_STAIRS', '통로를 따라 은주1관 1층 복도 끝계단으로 이동합니다.', 94),
    ('HL03_MAIN_LINK', 's1014_101449', '통로를 따라 지하주차장 본관통로 앞으로 이동합니다.', 332);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM hanlim_new_connections c
        LEFT JOIN nodes f ON f.node_id = c.from_node_id
        LEFT JOIN nodes t ON t.node_id = c.to_node_id
        WHERE f.node_id IS NULL OR t.node_id IS NULL
    ) THEN
        RAISE EXCEPTION '추가 연결에 필요한 노드가 없습니다. 가상 노드를 생성하지 않고 중단합니다.';
    END IF;
    IF EXISTS (SELECT 1 FROM places WHERE arrive_node_id = 'HL03_P1') THEN
        RAISE EXCEPTION 'HL03_P1에 연결된 장소가 있습니다. 장소 매핑을 확인하기 전에는 삭제하지 않습니다.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM edges
        WHERE (from_node_id = 'HL03_P1' AND to_node_id NOT IN ('HL03_DINING_STAIRS_FRONT','HL03_EUNJU_LINK','HL03_LOUNGE'))
           OR (to_node_id = 'HL03_P1' AND from_node_id NOT IN ('HL03_DINING_STAIRS_FRONT','HL03_EUNJU_LINK','HL03_LOUNGE'))
    ) THEN
        RAISE EXCEPTION 'HL03_P1에 확인되지 않은 추가 간선이 있습니다. 삭제 범위를 검토하세요.';
    END IF;
END $$;

DELETE FROM edges
WHERE from_node_id = 'HL03_P1' OR to_node_id = 'HL03_P1'
   OR (from_node_id = 'HL03_HALL' AND to_node_id = 'HL03_MAIN_PATH')
   OR (from_node_id = 'HL03_MAIN_PATH' AND to_node_id = 'HL03_HALL');

-- AI 검색 테이블이 초기화된 환경에서만 삭제 노드의 검색 문서를 정리한다.
-- 검색 기능이 아직 설치되지 않은 DB에서는 존재하지 않는 테이블을 참조하지 않는다.
DO $$
BEGIN
    IF to_regclass('search_documents') IS NOT NULL THEN
        LOCK TABLE search_documents IN SHARE ROW EXCLUSIVE MODE;
        DELETE FROM search_documents WHERE doc_type = 'NODE' AND ref_id = 'HL03_P1';
    END IF;
END $$;
DELETE FROM nodes WHERE node_id = 'HL03_P1';

-- 건물·층·사진명은 확정된 노드 테이블에서 읽어 기존 표기를 유지한다.
INSERT INTO edges (from_node_id, from_building, from_floor, from_photo,
                   to_node_id, to_building, to_floor, to_photo, move_type, description, arrow_yaw_deg)
SELECT f.node_id, f.building, f.floor_label, f.original_filename,
       t.node_id, t.building, t.floor_label, t.original_filename, 'WALK', c.description, c.arrow_yaw_deg
FROM hanlim_new_connections c
JOIN nodes f ON f.node_id = c.from_node_id
JOIN nodes t ON t.node_id = c.to_node_id
ON CONFLICT (from_node_id, to_node_id, move_type) DO NOTHING;

-- 신규 역방향 각도·안내문은 미지정하여 NULL로 둔다. 기존 역방향 값은 보존한다.
-- 기존 은주1관 2층 연결은 삭제하지 않는다. 180도 보정 등도 추정하지 않는다.
INSERT INTO edges (from_node_id, from_building, from_floor, from_photo,
                   to_node_id, to_building, to_floor, to_photo, move_type, description, arrow_yaw_deg)
SELECT t.node_id, t.building, t.floor_label, t.original_filename,
       f.node_id, f.building, f.floor_label, f.original_filename, 'WALK', NULL, NULL
FROM hanlim_new_connections c
JOIN nodes f ON f.node_id = c.from_node_id
JOIN nodes t ON t.node_id = c.to_node_id
ON CONFLICT (from_node_id, to_node_id, move_type) DO NOTHING;
