-- 새 연결 CSV에서 추출한 노드 구조. 간선·장소 구조는 후속 작업에서 변경한다.
BEGIN;

CREATE TABLE IF NOT EXISTS nodes (
    node_id text PRIMARY KEY,
    building text NOT NULL,
    floor_label text,
    original_filename text NOT NULL
);

CREATE TABLE IF NOT EXISTS edges (
    from_node_id text NOT NULL REFERENCES nodes(node_id),
    from_building text,
    from_floor text,
    from_photo text,
    to_node_id text NOT NULL REFERENCES nodes(node_id),
    to_building text,
    to_floor text,
    to_photo text,
    move_type text NOT NULL,
    description text,
    PRIMARY KEY (from_node_id, to_node_id, move_type),
    CHECK (from_node_id <> to_node_id)
);

CREATE TABLE IF NOT EXISTS places (
    place_id text PRIMARY KEY,
    name text NOT NULL,
    kind text,
    building text,
    floor_label text,
    room_no text,
    arrive_node_id text REFERENCES nodes(node_id),
    arrive_node_title text,
    door_photo text,
    mapping_basis text,
    aliases text,
    sub text
);

CREATE INDEX IF NOT EXISTS idx_edges_to_node ON edges(to_node_id);
CREATE INDEX IF NOT EXISTS idx_places_arrive_node ON places(arrive_node_id);
CREATE INDEX IF NOT EXISTS idx_nodes_building_floor ON nodes(building, floor_label);
COMMIT;
