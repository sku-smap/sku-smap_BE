-- V5에서 NULL로 남긴 역방향 6개의 안내문·각도를 사용자 확정값으로 채운다.
-- 노드·간선 추가·삭제 및 나머지 필드 변경은 하지 않는다.
LOCK TABLE edges IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE hanlim_reverse_guidance (
    from_node_id text NOT NULL,
    to_node_id text NOT NULL,
    description text NOT NULL,
    arrow_yaw_deg double precision NOT NULL,
    PRIMARY KEY (from_node_id, to_node_id)
) ON COMMIT DROP;

INSERT INTO hanlim_reverse_guidance VALUES
    ('HL03_EUNJU_LINK', 'HL03_DINING_STAIRS_FRONT', '통로를 따라 한림관 3층 식당계단 앞으로 이동합니다.', 260),
    ('HL03_MAIN_PATH', 'HL03_DINING_STAIRS_FRONT', '통로를 따라 한림관 3층 식당계단 앞으로 이동합니다.', 315),
    ('HL03_HALL', 'HL03_EUNJU_LINK', '통로를 따라 한림관 3층 은주1관연결 통로로 이동합니다.', 63),
    ('HL03_LOUNGE', 'HL03_HALL', '통로를 따라 한림관 3층 복도로 이동합니다.', 53),
    ('EJ1_01_END_STAIRS', 'HL03_EUNJU_STAIRS', '통로를 따라 한림관 3층 은주1관 연결통로계단으로 이동합니다.', 257),
    ('s1014_101449', 'HL03_MAIN_LINK', '통로를 따라 한림관 3층 본관한림관 연결길로 이동합니다.', 323);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM hanlim_reverse_guidance m
        LEFT JOIN edges e ON e.from_node_id=m.from_node_id AND e.to_node_id=m.to_node_id AND e.move_type='WALK'
        WHERE e.from_node_id IS NULL
    ) THEN
        RAISE EXCEPTION '역방향 WALK 간선이 없습니다. V5 적용 및 연결 정보를 확인하세요.';
    END IF;
END $$;

UPDATE edges e SET description=m.description, arrow_yaw_deg=m.arrow_yaw_deg
FROM hanlim_reverse_guidance m
WHERE e.from_node_id=m.from_node_id AND e.to_node_id=m.to_node_id AND e.move_type='WALK'
  AND (e.description IS DISTINCT FROM m.description OR e.arrow_yaw_deg IS DISTINCT FROM m.arrow_yaw_deg);
