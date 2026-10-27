package com.smap.domain.place;

import com.smap.global.external.s3.PhotoUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 HTTP 처리·서비스·저장소를 H2에서 검증하며 테스트 데이터는 롤백한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlaceAndNodeApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PhotoUrlService photos;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename) VALUES (?,?,?,?)",
                "search-node-a", "은주1관", "2", "은주1관_2층_엘베앞.jpg");
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename) VALUES (?,?,?,?)",
                "search-node-b", "본관", "1", "본관_1층_입구앞.jpg");
        insert("search-place-a", "도시공학과 실습실", "은주1관", "202", "URBAN LAB | 실습 공간", "search-node-a");
        insert("search-place-b", "강의실", "본관", "101", null, "search-node-b");
        insert("search-unlinked", "도시공학과 미연결 장소", "은주1관", "203", null, null);
    }

    @Test
    void searchesNameAliasBuildingAndRoomWithUsableNodeIds() throws Exception {
        for (String keyword : new String[] {"도시공학과", "urban lab", "은주1관", "202", "  실습 공간  ",
                java.text.Normalizer.normalize("도시공학과", java.text.Normalizer.Form.NFD)}) {
            mvc.perform(get("/api/v1/places").param("keyword", keyword))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].placeId").value("search-place-a"))
                    .andExpect(jsonPath("$.data[0].arriveNodeId").value("search-node-a"))
                    .andExpect(jsonPath("$.data[0].mappingBasis").doesNotExist());
        }
        verifyNoInteractions(photos);
    }

    @Test
    void missingOrBlankKeywordListsAllConnectedPlaces() throws Exception {
        mvc.perform(get("/api/v1/places"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/v1/places").param("keyword", "  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void unknownAndWildcardKeywordsReturnEmptyArray() throws Exception {
        for (String keyword : new String[] {"없는장소", "%", "_", "' OR 1=1 --"}) {
            mvc.perform(get("/api/v1/places").param("keyword", keyword))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Test
    void nodesAreOrderedAndDoNotRequireS3OrEdges() throws Exception {
        mvc.perform(get("/api/v1/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].nodeId").value("search-node-a"))
                .andExpect(jsonPath("$.data[0].title").value("은주1관 2층 엘베앞"))
                .andExpect(jsonPath("$.data[0].originalFilename").value("은주1관_2층_엘베앞.jpg"))
                .andExpect(jsonPath("$.data[1].nodeId").value("search-node-b"))
                .andExpect(jsonPath("$.data[0].imageUrl").doesNotExist());
        verifyNoInteractions(photos);
    }

    @Test
    void endpointsAreIncludedInSwagger() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/places'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/nodes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/campus-map'].get").exists());
    }

    /** 원본 방향·복수 이동수단·장소·사진 URL을 모두 유지하는 둘러보기 응답이다. */
    @Test
    void campusMapReturnsAllNodesEdgesAndMappedPlaces() throws Exception {
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename) VALUES (?,?,?,?)",
                "search-node-c", "본관", "2", "본관_2층_엘베앞.jpg");
        insert("search-place-c", "추가 실습실", "은주1관", "204", null, "search-node-a");
        for (String type : new String[] {"WALK", "ELEVATOR"}) {
            jdbc.update("INSERT INTO edges(from_node_id,to_node_id,move_type,description) VALUES (?,?,?,?)",
                    "search-node-a", "search-node-b", type, "본관으로 이동합니다.");
        }
        jdbc.update("INSERT INTO edges(from_node_id,to_node_id,move_type,description) VALUES (?,?,?,?)",
                "search-node-b", "search-node-c", "ELEVATOR", "2층으로 이동합니다.");
        when(photos.createUrl(anyString())).thenReturn("https://example.invalid/photo.jpg");

        mvc.perform(get("/api/v1/campus-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.totalEdges").value(3))
                .andExpect(jsonPath("$.data.totalPlaces").value(3))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value("search-node-a"))
                .andExpect(jsonPath("$.data.nodes[0].places", hasSize(2)))
                .andExpect(jsonPath("$.data.nodes[0].imageUrl").value("https://example.invalid/photo.jpg"))
                .andExpect(jsonPath("$.data.nodes[1].places[0].arriveNodeId").value("search-node-b"))
                .andExpect(jsonPath("$.data.nodes[2].places").isEmpty())
                .andExpect(jsonPath("$.data.edges[?(@.moveType == 'ELEVATOR')]", hasSize(2)))
                .andExpect(jsonPath("$.data.edges[?(@.fromNodeId == 'search-node-c')]").isEmpty())
                .andExpect(jsonPath("$.data.edges[?(@.toNodeId == 'search-node-a')]").isEmpty());
        verify(photos, times(3)).createUrl(anyString());
    }

    /** 고립 노드도 보존하고 빈 이동 유형은 null로 유지한다. */
    @Test
    void campusMapKeepsUnconnectedNodesAndBlankMoveType() throws Exception {
        mvc.perform(get("/api/v1/campus-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNodes").value(2))
                .andExpect(jsonPath("$.data.edges").isEmpty());
        jdbc.update("INSERT INTO edges(from_node_id,to_node_id,move_type,description) VALUES (?,?,?,?)",
                "search-node-a", "search-node-b", "", "이동합니다.");
        mvc.perform(get("/api/v1/campus-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEdges").value(1))
                .andExpect(jsonPath("$.data.edges[0].moveType").isEmpty())
                .andExpect(jsonPath("$.data.edges[0].description").value("이동합니다."));
    }

    private void insert(String id, String name, String building, String room, String aliases, String node) {
        jdbc.update("INSERT INTO places(place_id,name,building,room_no,aliases,arrive_node_id) VALUES (?,?,?,?,?,?)",
                id, name, building, room, aliases, node);
    }
}
