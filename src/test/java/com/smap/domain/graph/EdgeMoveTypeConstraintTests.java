package com.smap.domain.graph;

import com.smap.domain.graph.service.GraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** 저장 허용값과 빈 문자열을 null로 조회하는 계약을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EdgeMoveTypeConstraintTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired GraphService graphs;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO nodes(node_id,building,original_filename) VALUES ('type-a','본관','a.jpg'),('type-b','본관','b.jpg')");
    }

    @Test
    void emptyAndEnumTypesAreAcceptedAndEmptyLoadsAsNull() {
        for (String type : new String[] {"", "WALK", "STAIRS", "ELEVATOR", "DOOR"}) insert(type);
        var segments = graphs.load().outgoing().get("type-a");
        assertEquals(5, segments.size());
        assertEquals(1, segments.stream().filter(edge -> edge.moveType() == null).count());
    }

    @Test
    void unknownTypeCannotBeStored() {
        assertThrows(DataIntegrityViolationException.class, () -> insert("FLY"));
    }

    @Test
    void nonCanonicalTypeCannotBeStored() {
        assertThrows(DataIntegrityViolationException.class, () -> insert(" walk "));
    }

    private void insert(String type) {
        jdbc.update("INSERT INTO edges(from_node_id,to_node_id,move_type) VALUES ('type-a','type-b',?)", type);
    }
}
