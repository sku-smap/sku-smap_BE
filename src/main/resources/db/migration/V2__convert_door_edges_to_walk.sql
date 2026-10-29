-- 출입문 간선은 일반 도보로 통일한다. ID·방향·안내문 등 나머지 값은 보존한다.
-- 검사 후 UPDATE 사이의 동시 쓰기를 막는다. Flyway 트랜잭션 종료 시 해제된다.
LOCK TABLE edges IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM edges d
        JOIN edges w ON w.from_node_id = d.from_node_id AND w.to_node_id = d.to_node_id
        WHERE d.move_type = 'DOOR' AND w.move_type = 'WALK'
    ) THEN
        RAISE EXCEPTION 'DOOR를 WALK로 변경하면 기존 WALK 간선과 기본키가 충돌합니다. 간선을 삭제하지 않고 중단합니다.';
    END IF;
END $$;

UPDATE edges SET move_type = 'WALK' WHERE move_type = 'DOOR';
