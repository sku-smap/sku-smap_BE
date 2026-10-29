package com.smap.domain.place;

import com.smap.global.external.s3.PhotoUrlService;
import com.smap.global.external.s3.ThumbnailUrlService;
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
    @MockitoBean ThumbnailUrlService thumbnails;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename) VALUES (?,?,?,?)",
                "search-node-a", "은주1관", "2", "은주1관_2층_엘베앞.jpg");
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename) VALUES (?,?,?,?)",
                "search-node-b", "본관", "1", "본관_1층_입구앞.jpg");
        insert("search-place-a", "도시공학과 실습실", "은주1관", "202", "URBAN LAB | 실습 공간", "search-node-a");
        insert("search-place-b", "강의실", "본관", "101", null, "search-node-b");
        jdbc.update("UPDATE places SET door_photo = ? WHERE place_id = ?", "door/example.jpg", "search-place-a");
        insert("search-unlinked", "도시공학과 미연결 장소", "은주1관", "203", null, null);
        jdbc.update("UPDATE nodes SET building_id = CASE building WHEN '본관' THEN 'BON' ELSE 'EUNJU1' END");
        jdbc.update("UPDATE places SET building_id = CASE building WHEN '본관' THEN 'BON' ELSE 'EUNJU1' END");
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
                    .andExpect(jsonPath("$.data[0].doorPhoto").doesNotExist())
                    .andExpect(jsonPath("$.data[0].buildingId").value("EUNJU1"))
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
                .andExpect(jsonPath("$.data[0].photoType").value("PANORAMA"))
                .andExpect(jsonPath("$.data[0].buildingId").value("EUNJU1"))
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
                .andExpect(jsonPath("$.paths['/api/v1/buildings'].get").exists())
                .andExpect(jsonPath("$.components.schemas.PlaceKind.enum").value(
                        org.hamcrest.Matchers.containsInAnyOrder("room", "professor", "office", "facility", "building")))
                .andExpect(jsonPath("$.paths['/api/v1/campus-map'].get").exists());
    }

    /** 두 테이블의 중복을 제거하고 같은 야외 표시명을 가진 서로 다른 ID를 보존한다. */
    @Test
    void buildingsReturnDistinctIdsAndDisplayNames() throws Exception {
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename,building_id) VALUES (?,?,?,?,?)",
                "ground", "야외", "지상", "ground.jpg", "OUTDOOR_GROUND");
        jdbc.update("INSERT INTO nodes(node_id,building,floor_label,original_filename,building_id) VALUES (?,?,?,?,?)",
                "underground", "야외", "B1층", "underground.jpg", "OUTDOOR_UNDERGROUND");
        jdbc.update("INSERT INTO places(place_id,name,building,building_id) VALUES (?,?,?,?)",
                "place-only", "수인관", "수인관", "SUIN");
        mvc.perform(get("/api/v1/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].buildingId").value("BON"))
                .andExpect(jsonPath("$.data[0].building").value("본관"))
                .andExpect(jsonPath("$.data[1].buildingId").value("EUNJU1"))
                .andExpect(jsonPath("$.data[2].buildingId").value("OUTDOOR_GROUND"))
                .andExpect(jsonPath("$.data[2].building").value("야외"))
                .andExpect(jsonPath("$.data[3].buildingId").value("OUTDOOR_UNDERGROUND"))
                .andExpect(jsonPath("$.data[3].building").value("야외"))
                .andExpect(jsonPath("$.data[4].buildingId").value("SUIN"));
        verifyNoInteractions(photos);
    }

    @Test
    void emptyBuildingsReturnEmptyArray() throws Exception {
        jdbc.update("DELETE FROM places");
        jdbc.update("DELETE FROM nodes");
        mvc.perform(get("/api/v1/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
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
        when(thumbnails.createUrl("은주1관_2층_엘베앞.jpg")).thenReturn("https://example.invalid/thumbs/photo.jpg");

        mvc.perform(get("/api/v1/campus-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.totalEdges").value(3))
                .andExpect(jsonPath("$.data.totalPlaces").value(3))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value("search-node-a"))
                .andExpect(jsonPath("$.data.nodes[0].photoType").value("PANORAMA"))
                .andExpect(jsonPath("$.data.nodes[0].buildingId").value("EUNJU1"))
                .andExpect(jsonPath("$.data.nodes[0].places", hasSize(2)))
                .andExpect(jsonPath("$.data.nodes[0].places[*].doorPhoto").isEmpty())
                .andExpect(jsonPath("$.data.nodes[0].imageUrl").value("https://example.invalid/photo.jpg"))
                .andExpect(jsonPath("$.data.nodes[0].thumbnailUrl").value("https://example.invalid/thumbs/photo.jpg"))
                .andExpect(jsonPath("$.data.nodes[1].thumbnailUrl").isEmpty())
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

    /** 필드 존재 여부와 값의 null 허용 여부는 별개의 계약이다. */
    @Test
    void swaggerDistinguishesRequiredFieldsAndNullableValues() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var schemas = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("components").path("schemas");
        var place = schemas.path("PlaceResponseDTO");
        org.junit.jupiter.api.Assertions.assertEquals(place.path("properties").size(), place.path("required").size());
        org.junit.jupiter.api.Assertions.assertTrue(place.path("properties").path("sub").path("nullable").asBoolean());
        org.junit.jupiter.api.Assertions.assertTrue(place.path("properties").path("kind").path("nullable").asBoolean(),
                place.path("properties").path("kind").toString());
        org.junit.jupiter.api.Assertions.assertFalse(place.path("properties").path("aliases").path("nullable").asBoolean());
        org.junit.jupiter.api.Assertions.assertFalse(place.path("properties").path("arriveNodeId").path("nullable").asBoolean());
        var node = schemas.path("NodeResponseDTO");
        org.junit.jupiter.api.Assertions.assertEquals(node.path("properties").size(), node.path("required").size());
        org.junit.jupiter.api.Assertions.assertTrue(node.path("properties").path("floorLabel").path("nullable").asBoolean());
        org.junit.jupiter.api.Assertions.assertFalse(node.path("properties").path("nodeId").path("nullable").asBoolean());
        var request = schemas.path("RouteRequestDTO");
        org.junit.jupiter.api.Assertions.assertEquals(2, request.path("required").size());
        org.junit.jupiter.api.Assertions.assertTrue(request.path("properties").path("option").path("nullable").asBoolean());
        var iterator = schemas.fields();
        while (iterator.hasNext()) {
            var schema = iterator.next();
            if (schema.getKey().startsWith("ApiResponse")) {
                org.junit.jupiter.api.Assertions.assertEquals(4, schema.getValue().path("required").size());
                org.junit.jupiter.api.Assertions.assertTrue(schema.getValue().path("properties").path("data").path("nullable").asBoolean(), schema.getKey());
            }
        }
    }

    @Test
    void domainApisExplicitlyReturnJson() throws Exception {
        for (String path : new String[]{"/api/v1/buildings", "/api/v1/nodes", "/api/v1/places", "/api/v1/campus-map"}) {
            mvc.perform(get(path).accept(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON));
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/routes")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON));
    }

    /** DB 문자열은 보존하면서 세 API의 장소 필드를 같은 형식으로 제공한다. */
    @Test
    void aliasesAndKindsAreConsistentAcrossPlaceMapAndRoute() throws Exception {
        String original = " | URBAN LAB | 실습 공간 || URBAN LAB | ";
        jdbc.update("UPDATE places SET kind = 'room', aliases = ? WHERE place_id = 'search-place-a'", original);
        jdbc.update("INSERT INTO edges(from_node_id,to_node_id,move_type) VALUES (?,?,?)",
                "search-node-a", "search-node-b", "WALK");
        mvc.perform(get("/api/v1/places").param("keyword", "202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].kind").value("room"))
                .andExpect(jsonPath("$.data[0].aliases", hasSize(2)))
                .andExpect(jsonPath("$.data[0].aliases[0]").value("URBAN LAB"))
                .andExpect(jsonPath("$.data[0].aliases[1]").value("실습 공간"));
        mvc.perform(get("/api/v1/campus-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[0].places[0].kind").value("room"))
                .andExpect(jsonPath("$.data.nodes[0].places[0].aliases", hasSize(2)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/routes")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"fromNodeId\":\"search-node-a\",\"toNodeId\":\"search-node-b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[0].places[0].kind").value("room"))
                .andExpect(jsonPath("$.data.nodes[0].places[0].aliases", hasSize(2)));
        mvc.perform(get("/api/v1/places").param("keyword", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].aliases").isArray())
                .andExpect(jsonPath("$.data[0].aliases").isEmpty())
                .andExpect(jsonPath("$.data[0].kind").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(original,
                jdbc.queryForObject("SELECT aliases FROM places WHERE place_id = 'search-place-a'", String.class));
    }
}
