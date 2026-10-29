-- 사용자가 확정한 공간 표기. 노드와 연결은 생성·삭제하지 않는다.
CREATE TEMP TABLE floor_label_overrides (node_id text PRIMARY KEY, label text NOT NULL) ON COMMIT DROP;
INSERT INTO floor_label_overrides VALUES
    ('bus_stop', '지상'),
    ('daeil_chungwoon_fork', '지상'),
    ('daeil_deck', '지상'),
    ('daeilhs_gate', '지상'),
    ('daeilhs_gate_in', '지상'),
    ('EJ1_FRONT', '지상'),
    ('EJ2_CAFE', '지상'),
    ('EJ2_FRONT2', '지상'),
    ('eunju_suin_5way', '지상'),
    ('eunju2_men', '지상'),
    ('eunju2_women', '지상'),
    ('field', '지상'),
    ('futsal', '지상'),
    ('futsal_to_daeilhs', '지상'),
    ('gate_front', '지상'),
    ('hill_daeil_sp_stair', '지상'),
    ('hill_entrance2', '지상'),
    ('hill_field_entrance', '지상'),
    ('hill_moonye_3way', '지상'),
    ('hill_moonye_bon_4way', '지상'),
    ('hill_moonye_stair2', '지상'),
    ('hill_sp_3way', '지상'),
    ('hr_5way', '지상'),
    ('hr_futsal_back', '지상'),
    ('hr_road', '지상'),
    ('parking_entrance', '지상'),
    ('road_bon_2way', '지상'),
    ('road_daeilhs_4way', '지상'),
    ('road_hanlim_3way', '지상'),
    ('road_parking_4way', '지상'),
    ('road_yudam_3way', '지상'),
    ('road_yudam_front', '지상'),
    ('rotc', '지상'),
    ('s1012_160527', '지상'),
    ('s1012_160614', '지상'),
    ('s1013_110442', '지상'),
    ('s1013_110654', '지상'),
    ('s1013_110730', '지상'),
    ('s1013_111119', '지상'),
    ('s1014_101449', '지상'),
    ('suin_hall_in', '지상'),
    ('MY06_BU07_CONNECT', '연결계단'),
    ('s1013_102207', '지하');

-- 잘못된 ID나 예상하지 못한 누락은 묵인하지 않고 전체 작업을 중단한다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM floor_label_overrides o LEFT JOIN nodes n USING (node_id) WHERE n.node_id IS NULL) THEN
        RAISE EXCEPTION '층 표기 대상 노드가 존재하지 않습니다';
    END IF;
    IF EXISTS (SELECT 1 FROM nodes n WHERE n.floor_label IS NULL AND NOT EXISTS
        (SELECT 1 FROM floor_label_overrides o WHERE o.node_id = n.node_id)) THEN
        RAISE EXCEPTION '사용자 확인 목록에 없는 NULL 층 노드가 있습니다';
    END IF;
END $$;

UPDATE nodes n SET floor_label = o.label FROM floor_label_overrides o WHERE n.node_id = o.node_id;
-- 숫자·L·B층 표기에만 접미사를 붙여 지상·지하·연결계단은 유지한다.
UPDATE nodes SET floor_label = floor_label || '층' WHERE floor_label ~ '^([0-9]+|L|B[0-9]+)$';

-- 간선의 층은 연결된 노드의 층과 일치시킨다.
UPDATE edges e SET from_floor = n.floor_label FROM nodes n
WHERE e.from_node_id = n.node_id AND e.from_floor IS DISTINCT FROM n.floor_label;
UPDATE edges e SET to_floor = n.floor_label FROM nodes n
WHERE e.to_node_id = n.node_id AND e.to_floor IS DISTINCT FROM n.floor_label;

-- 장소 층은 도착 노드와 다를 수 있으므로 원래 층을 보존하며 표기만 통일한다.
UPDATE places SET floor_label = floor_label || '층' WHERE floor_label ~ '^([0-9]+|L|B[0-9]+)$';