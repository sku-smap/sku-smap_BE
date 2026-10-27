package com.smap.domain.route;

import com.smap.domain.graph.repository.EdgeRepository;
import com.smap.domain.graph.repository.NodeRepository;
import com.smap.domain.graph.service.GraphService;
import com.smap.domain.place.repository.PlaceRepository;
import com.smap.domain.route.service.RouteService;
import com.smap.domain.route.service.DijkstraPathFinder;
import com.smap.domain.route.exception.RouteException;
import com.smap.domain.route.exception.RouteErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** 새 노드·간선이 적재된 로컬 DB를 변경 없이 검증한다. 명시적으로 활성화한 경우에만 실행한다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/smap",
        "spring.datasource.username=smap",
        "spring.datasource.password=smap",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SMAP_POSTGRES_TEST", matches = "true")
class CampusPostgresTests {
    @Autowired NodeRepository nodes;
    @Autowired EdgeRepository edges;
    @Autowired PlaceRepository places;
    @Autowired GraphService graphs;
    @Autowired RouteService routes;
    @Autowired DijkstraPathFinder pathFinder;
    @Autowired JdbcTemplate jdbc;

    /** 4개 컬럼과 328개 노드, 사용자 확인 사진 및 변경된 북악관 ID를 확인한다. */
    @Test
    void mapsNewNodeSchemaAndConfirmedPhotos() {
        assertEquals(List.of("node_id", "building", "floor_label", "original_filename"),
                jdbc.queryForList("""
                        SELECT column_name FROM information_schema.columns
                        WHERE table_schema='public' AND table_name='nodes'
                        ORDER BY ordinal_position
                        """, String.class));
        assertEquals(328, nodes.count());
        assertEquals(918, edges.count());
        assertEquals(List.of("from_node_id", "from_building", "from_floor", "from_photo",
                        "to_node_id", "to_building", "to_floor", "to_photo", "move_type", "description"),
                jdbc.queryForList("""
                        SELECT column_name FROM information_schema.columns
                        WHERE table_schema='public' AND table_name='edges' ORDER BY ordinal_position
                        """, String.class));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM edges WHERE move_type=''", Long.class));
        assertEquals(969, places.count());
        assertFalse(nodes.existsById("bukak_1f_elev"));
        assertEquals("7", nodes.findById("bukak_7f_elev").orElseThrow().getFloorLabel());
        assertEquals("유담관_L층_중앙.jpg",
                nodes.findById("s1013_103441").orElseThrow().getOriginalFilename());
        assertEquals("문예관6층-북악관7층_연결계단.jpg",
                nodes.findById("MY06_BU07_CONNECT").orElseThrow().getOriginalFilename());
        assertTrue(nodes.findAll().stream().allMatch(n ->
                n.getNodeId() != null && n.getBuilding() != null && n.getOriginalFilename() != null));
    }

    /** 시간 컬럼 없이 CSV 연결·안내 문구와 실제 경로 결과를 검증한다. */
    @Test
    void loadsNodesWithoutInventingEdges() {
        var graph = graphs.load();
        assertEquals(328, graph.nodes().size());
        assertEquals(918, graph.outgoing().values().stream().mapToInt(List::size).sum());
        for (var segment : graph.outgoing().values().stream().flatMap(List::stream).toList()) {
            assertNotNull(segment.description());
            assertFalse(segment.description().isBlank());
        }
        var result = routes.find("bus_stop", "bus_stop");
        assertEquals(1, result.nodes().size());
        assertEquals(0, result.totalSteps());
        assertTrue(result.segments().isEmpty());
            var route = routes.find("bus_stop", "eunju2_men");
            assertEquals("bus_stop", route.nodes().get(0).nodeId());
            assertEquals("eunju2_men", route.nodes().get(route.nodes().size() - 1).nodeId());
            assertEquals(route.nodes().size() - 1, route.segments().size());
            assertEquals(route.segments().size(), route.totalSteps());
            assertTrue(route.segments().stream().allMatch(s -> s.description() != null));
    }

    /** 제외·보류 장소가 없고, 장소별 도착 노드 참조 및 북악관 ID 변경을 확인한다. */
    @Test
    void mapsPlacesToExistingNodesAndRespectsExclusions() {
        assertEquals(0L, jdbc.queryForObject("""
                SELECT count(*) FROM places p LEFT JOIN nodes n ON n.node_id=p.arrive_node_id
                WHERE n.node_id IS NULL
                """, Long.class));
        assertEquals(0L, jdbc.queryForObject("""
                SELECT count(*) FROM places WHERE (building='대일관' AND (floor_label IN ('6','7') OR place_id='f-studio'))
                """, Long.class));
        var mapped = places.findByArriveNodeIdOrderByNameAsc("bukak_7f_elev");
        assertEquals(18, mapped.size());
        // 한 지점에 연결된 여러 장소가 모두 응답에 포함되는지 실제 DB로 검증한다.
        var response = com.smap.domain.route.dto.response.RouteResponseDTO.from(
                routes.find("bukak_7f_elev", "bukak_7f_elev"));
        assertEquals(18, response.nodes().get(0).places().size());
        assertEquals(mapped.stream().map(p -> p.getPlaceId()).sorted().toList(),
                response.nodes().get(0).places().stream().map(p -> p.placeId()).sorted().toList());
        assertNull(response.nodes().get(0).description());
        assertTrue(mapped.stream().anyMatch(p -> p.getPlaceId().equals("r-bukak-707")));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM places WHERE arrive_node_id='bukak_1f_elev'", Long.class));
    }
}
