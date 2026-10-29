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
                            .schemas(schema).defaultSchema(schema).load();
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
