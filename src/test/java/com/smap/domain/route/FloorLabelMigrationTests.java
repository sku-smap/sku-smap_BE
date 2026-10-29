package com.smap.domain.route;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 로컬 PostgreSQL의 독립 스키마에서 기존 seed → Flyway 전환과 재시작을 검증한다. */
@EnabledIfEnvironmentVariable(named = "SMAP_POSTGRES_TEST", matches = "true")
class FloorLabelMigrationTests {
    @Test
    void migratesExistingDataOnceWithoutChangingConnections() throws Exception {
        String schema = "floor_test_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:postgresql://localhost:5432/smap";
        try (Connection connection = DriverManager.getConnection(url, "smap", "smap")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + schema);
                connection.setSchema(schema);
                try {
                    for (String file : new String[]{"02-campus-schema.sql", "03-nodes-seed.sql",
                            "04-edges-seed.sql", "05-places-seed.sql"}) {
                        statement.execute(Files.readString(Path.of("db/init", file)));
                    }
                    String edgeFingerprint = scalar(connection, "SELECT md5(string_agg((to_jsonb(e) - 'from_floor' - 'to_floor')::text, '' ORDER BY from_node_id, to_node_id, move_type)) FROM edges e");
                    var flyway = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).baselineOnMigrate(true).baselineVersion("0")
                            .target("1").load();
                    assertEquals(1, flyway.migrate().migrationsExecuted);
                    assertEquals(0, flyway.migrate().migrationsExecuted);
                    assertEquals("328", scalar(connection, "SELECT count(*) FROM nodes"));
                    assertEquals("918", scalar(connection, "SELECT count(*) FROM edges"));
                    assertEquals("969", scalar(connection, "SELECT count(*) FROM places"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM nodes WHERE floor_label IS NULL"));
                    assertEquals("41", scalar(connection, "SELECT count(*) FROM nodes WHERE floor_label = '지상'"));
                    assertEquals("지하", scalar(connection, "SELECT floor_label FROM nodes WHERE node_id = 's1013_102207'"));
                    assertEquals("연결계단", scalar(connection, "SELECT floor_label FROM nodes WHERE node_id = 'MY06_BU07_CONNECT'"));
                    assertEquals("285", scalar(connection, "SELECT count(*) FROM nodes WHERE floor_label LIKE '%층'"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges e JOIN nodes n ON n.node_id = e.from_node_id WHERE e.from_floor IS DISTINCT FROM n.floor_label"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges e JOIN nodes n ON n.node_id = e.to_node_id WHERE e.to_floor IS DISTINCT FROM n.floor_label"));
                    assertEquals(edgeFingerprint, scalar(connection, "SELECT md5(string_agg((to_jsonb(e) - 'from_floor' - 'to_floor')::text, '' ORDER BY from_node_id, to_node_id, move_type)) FROM edges e"));
                    // V2는 기존 간선을 지우지 않고 이동 유형만 바꿔야 한다.
                    String doorIndependentFingerprint = scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(e) - 'move_type')::text, '' ORDER BY (to_jsonb(e) - 'move_type')::text)) FROM edges e");
                    assertEquals("14", scalar(connection, "SELECT count(*) FROM edges WHERE move_type = 'DOOR'"));
                    var latest = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).target("2").load();
                    // 운영 데이터에 중복이 생긴 경우 임의 삭제 없이 V2 전체가 롤백되어야 한다.
                    statement.execute("INSERT INTO edges(from_node_id, to_node_id, move_type) VALUES ('bon_1f_front', 'bon_1f_in', 'WALK')");
                    assertThrows(org.flywaydb.core.api.FlywayException.class, latest::migrate);
                    assertEquals("14", scalar(connection, "SELECT count(*) FROM edges WHERE move_type = 'DOOR'"));
                    assertEquals("919", scalar(connection, "SELECT count(*) FROM edges"));
                    statement.execute("DELETE FROM edges WHERE from_node_id = 'bon_1f_front' AND to_node_id = 'bon_1f_in' AND move_type = 'WALK'");
                    assertEquals(1, latest.migrate().migrationsExecuted);
                    assertEquals(0, latest.migrate().migrationsExecuted);
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges WHERE move_type = 'DOOR'"));
                    assertEquals("601", scalar(connection, "SELECT count(*) FROM edges WHERE move_type = 'WALK'"));
                    assertEquals("918", scalar(connection, "SELECT count(*) FROM edges"));
                    assertEquals(doorIndependentFingerprint, scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(e) - 'move_type')::text, '' ORDER BY (to_jsonb(e) - 'move_type')::text)) FROM edges e"));
                    String nodeFingerprint = scalar(connection,
                            "SELECT md5(string_agg(to_jsonb(n)::text, '' ORDER BY node_id)) FROM nodes n");
                    String placeFingerprint = scalar(connection,
                            "SELECT md5(string_agg(to_jsonb(p)::text, '' ORDER BY place_id)) FROM places p");
                    var buildingMigration = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).target("3").load();
                    assertEquals(1, buildingMigration.migrate().migrationsExecuted);
                    assertEquals(0, buildingMigration.migrate().migrationsExecuted);
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM nodes WHERE building_id IS NULL"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM places WHERE building_id IS NULL"));
                    assertEquals("BON", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 'bon_1f_front'"));
                    assertEquals("OUTDOOR_GROUND", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 'field'"));
                    assertEquals("OUTDOOR_GROUND", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 's1013_102040'"));
                    assertEquals("OUTDOOR_GROUND", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 's1014_101449'"));
                    assertEquals("OUTDOOR_UNDERGROUND", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 's1013_102207'"));
                    assertEquals("OUTDOOR_UNDERGROUND", scalar(connection, "SELECT building_id FROM nodes WHERE node_id = 's1014_100922'"));
                    assertEquals("OUTDOOR_UNDERGROUND", scalar(connection, "SELECT building_id FROM places WHERE place_id = 'f-parking1'"));
                    assertEquals("OUTDOOR_GROUND", scalar(connection, "SELECT building_id FROM places WHERE place_id = 'f-bus'"));
                    assertEquals(nodeFingerprint, scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(n) - 'building_id')::text, '' ORDER BY node_id)) FROM nodes n"));
                    assertEquals(placeFingerprint, scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(p) - 'building_id')::text, '' ORDER BY place_id)) FROM places p"));
                    String beforeArrows = scalar(connection,
                            "SELECT md5(string_agg(to_jsonb(e)::text, '' ORDER BY from_node_id,to_node_id,move_type)) FROM edges e");
                    // 운영에 먼저 적용한 뒤 다음 배포에서 Flyway가 실행되어도 안전해야 한다.
                    connection.setAutoCommit(false);
                    statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V4__add_edge_arrow_yaw.sql")));
                    connection.commit();
                    connection.setAutoCommit(true);
                    var arrowMigration = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).target("4").load();
                    assertEquals(1, arrowMigration.migrate().migrationsExecuted);
                    assertEquals(0, arrowMigration.migrate().migrationsExecuted);
                    assertEquals("918", scalar(connection, "SELECT count(*) FROM edges"));
                    assertEquals("914", scalar(connection, "SELECT count(*) FROM edges WHERE arrow_yaw_deg IS NOT NULL"));
                    assertEquals("4", scalar(connection, "SELECT count(*) FROM edges WHERE arrow_yaw_deg IS NULL"));
                    assertEquals("303", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='field' AND to_node_id='hill_field_entrance' AND move_type='WALK'"));
                    assertEquals(beforeArrows, scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(e) - 'arrow_yaw_deg')::text, '' ORDER BY from_node_id,to_node_id,move_type)) FROM edges e"));
                    assertThrows(java.sql.SQLException.class, () -> statement.execute(
                            "UPDATE edges SET arrow_yaw_deg=360 WHERE from_node_id='field' AND to_node_id='hill_field_entrance'"));
                    // 검색 테이블이 없는 로컬 환경에서도 V5가 동작해야 한다.
                    connection.setAutoCommit(false);
                    try {
                        statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V5__revise_hanlim_connections.sql")));
                        assertEquals("327", scalar(connection, "SELECT count(*) FROM nodes"));
                        assertEquals("922", scalar(connection, "SELECT count(*) FROM edges"));
                        assertEquals("0", scalar(connection, "SELECT count(*) FROM nodes WHERE node_id='HL03_P1'"));
                    } finally {
                        connection.rollback();
                        connection.setAutoCommit(true);
                    }
                    // 검색 테이블이 있는 환경에서는 해당 노드 문서만 정리한다.
                    statement.execute("CREATE TABLE search_documents(doc_type text, ref_id text, node_id text)");
                    statement.execute("INSERT INTO search_documents VALUES ('NODE','HL03_P1','HL03_P1'),('NODE','HL03_HALL','HL03_HALL')");
                    var hanlimMigration = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).target("5").load();
                    // 장소 참조가 새로 생겼다면 삭제를 중단하고 원본 데이터가 남아 있어야 한다.
                    statement.execute("INSERT INTO places(place_id,name,arrive_node_id,building_id) VALUES ('test-p1-reference','test','HL03_P1','HANLIM')");
                    assertThrows(org.flywaydb.core.api.FlywayException.class, hanlimMigration::migrate);
                    assertEquals("1", scalar(connection, "SELECT count(*) FROM nodes WHERE node_id='HL03_P1'"));
                    assertEquals("918", scalar(connection, "SELECT count(*) FROM edges"));
                    statement.execute("DELETE FROM places WHERE place_id='test-p1-reference'");
                    // 운영 선적용 뒤 Flyway 재적용 시 중복 추가나 기존 역방향 초기화가 없어야 한다.
                    connection.setAutoCommit(false);
                    statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V5__revise_hanlim_connections.sql")));
                    connection.commit();
                    connection.setAutoCommit(true);
                    assertEquals(1, hanlimMigration.migrate().migrationsExecuted);
                    assertEquals(0, hanlimMigration.migrate().migrationsExecuted);
                    assertEquals("327", scalar(connection, "SELECT count(*) FROM nodes"));
                    assertEquals("922", scalar(connection, "SELECT count(*) FROM edges"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM nodes WHERE node_id='HL03_P1'"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges WHERE from_node_id='HL03_P1' OR to_node_id='HL03_P1'"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges WHERE (from_node_id='HL03_HALL' AND to_node_id='HL03_MAIN_PATH') OR (to_node_id='HL03_HALL' AND from_node_id='HL03_MAIN_PATH')"));
                    assertEquals("332", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='HL03_MAIN_LINK' AND to_node_id='s1014_101449' AND move_type='WALK'"));
                    assertEquals("94", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='HL03_EUNJU_STAIRS' AND to_node_id='EJ1_01_END_STAIRS' AND move_type='WALK'"));
                    assertEquals("6", scalar(connection, "SELECT count(*) FROM edges WHERE arrow_yaw_deg IS NULL"));
                    assertEquals("193", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='EJ1_02_END_STAIRS' AND to_node_id='HL03_EUNJU_STAIRS' AND move_type='WALK'"));
                    assertEquals("2", scalar(connection, "SELECT count(*) FROM edges WHERE (from_node_id='HL03_EUNJU_STAIRS' AND to_node_id='EJ1_01_END_STAIRS') OR (to_node_id='HL03_EUNJU_STAIRS' AND from_node_id='EJ1_01_END_STAIRS')"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM search_documents WHERE ref_id='HL03_P1'"));
                    assertEquals("1", scalar(connection, "SELECT count(*) FROM search_documents WHERE ref_id='HL03_HALL'"));
                    assertEquals("0", scalar(connection, "WITH RECURSIVE reachable(id) AS (SELECT 'field'::text UNION SELECT e.to_node_id FROM edges e JOIN reachable r ON e.from_node_id=r.id) SELECT count(*) FROM nodes n WHERE n.node_id NOT IN (SELECT id FROM reachable)"));
                    assertEquals("0", scalar(connection, "WITH RECURSIVE reachable(id) AS (SELECT 'field'::text UNION SELECT e.from_node_id FROM edges e JOIN reachable r ON e.to_node_id=r.id) SELECT count(*) FROM nodes n WHERE n.node_id NOT IN (SELECT id FROM reachable)"));
                    String beforeReverseGuidance = scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(e) - 'description' - 'arrow_yaw_deg')::text, '' ORDER BY from_node_id,to_node_id,move_type)) FROM edges e");
                    connection.setAutoCommit(false);
                    statement.execute(Files.readString(Path.of("src/main/resources/db/migration/V6__fill_hanlim_reverse_guidance.sql")));
                    connection.commit();
                    connection.setAutoCommit(true);
                    var reverseMigration = Flyway.configure().dataSource(url, "smap", "smap")
                            .schemas(schema).defaultSchema(schema).load();
                    assertEquals(1, reverseMigration.migrate().migrationsExecuted);
                    assertEquals(0, reverseMigration.migrate().migrationsExecuted);
                    assertEquals("922", scalar(connection, "SELECT count(*) FROM edges"));
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM edges WHERE arrow_yaw_deg IS NULL"));
                    assertEquals("257", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='EJ1_01_END_STAIRS' AND to_node_id='HL03_EUNJU_STAIRS' AND move_type='WALK'"));
                    assertEquals("94", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='HL03_EUNJU_STAIRS' AND to_node_id='EJ1_01_END_STAIRS' AND move_type='WALK'"));
                    assertEquals("193", scalar(connection, "SELECT arrow_yaw_deg FROM edges WHERE from_node_id='EJ1_02_END_STAIRS' AND to_node_id='HL03_EUNJU_STAIRS' AND move_type='WALK'"));
                    assertEquals(beforeReverseGuidance, scalar(connection,
                            "SELECT md5(string_agg((to_jsonb(e) - 'description' - 'arrow_yaw_deg')::text, '' ORDER BY from_node_id,to_node_id,move_type)) FROM edges e"));
                } finally {
                    statement.execute("ROLLBACK");
                    connection.setSchema("public");
                    // 이 테스트에서 생성한 UUID 스키마만 제거한다.
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            }
        }
    }

    private String scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
