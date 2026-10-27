package com.smap.domain.graph;

import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.smap.domain.graph.service.GraphSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 빈 캠퍼스 그래프도 사진 URL 호출 없이 정상 반환한다. */
class CampusMapResponseTests {
    @Test
    void emptyGraphDoesNotRequestPhotoUrls() {
        var result = CampusMapResponseDTO.from(new GraphSnapshot(Map.of(), Map.of()), List.of(), filename -> {
            fail("노드가 없으면 사진 URL을 요청하면 안 됩니다.");
            return null;
        });
        assertTrue(result.nodes().isEmpty());
        assertTrue(result.edges().isEmpty());
        assertEquals(0, result.totalNodes());
        assertEquals(0, result.totalEdges());
        assertEquals(0, result.totalPlaces());
    }
}
