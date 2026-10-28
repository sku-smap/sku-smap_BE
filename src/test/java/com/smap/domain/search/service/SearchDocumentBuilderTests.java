package com.smap.domain.search.service;

import com.smap.domain.graph.repository.EdgeRepository;
import com.smap.domain.graph.repository.NodeRepository;
import com.smap.domain.place.entity.Place;
import com.smap.domain.place.repository.PlaceRepository;
import com.smap.domain.search.model.SearchDocument;
import com.smap.domain.search.repository.SearchDocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchDocumentBuilderTests {

    @Test
    void nodeTitleRemovesExtensionAndUnderscores() {
        assertEquals("북악관 지하1층 엘베 앞", SearchDocumentBuilder.nodeTitle("북악관_지하1층_엘베앞.jpg"));
    }

    @Test
    void nodeTitleRemovesTrailingNumberAfterFloor() {
        assertEquals("한림관 12층", SearchDocumentBuilder.nodeTitle("한림관_12층1.jpg"));
    }

    @Test
    void nodeTitleRemovesUnderscoreSequence() {
        assertEquals("지하1 4갈래(2115버정대기,북악관,본관)",
                SearchDocumentBuilder.nodeTitle("지하1_4갈래(2115버정대기,북악관,본관)_002.jpg"));
    }

    @Test
    void edgeTargetAfterPathVerb() {
        assertEquals("북악관 엘리베이터 앞",
                SearchDocumentBuilder.edgeTarget("복도를 따라 북악관 엘리베이터 앞으로 이동합니다."));
        assertEquals("북악관 7층",
                SearchDocumentBuilder.edgeTarget("엘리베이터를 타고 북악관 7층으로 내려갑니다."));
    }

    @Test
    void edgeTargetFloorOnlyIsNull() {
        assertNull(SearchDocumentBuilder.edgeTarget("엘리베이터를 타고 12층으로 내려갑니다."));
    }

    @Test
    void edgeTargetWithoutPrefix() {
        assertEquals("버스정류장", SearchDocumentBuilder.edgeTarget("버스정류장으로 이동합니다."));
    }

    @Test
    void edgeTargetEmptyOrNull() {
        assertNull(SearchDocumentBuilder.edgeTarget(""));
        assertNull(SearchDocumentBuilder.edgeTarget(null));
    }

    @Test
    void floorTextFormats() {
        assertEquals("지하1층", SearchDocumentBuilder.floorText("B1"));
        assertEquals("L층", SearchDocumentBuilder.floorText("L"));
        assertEquals("7층", SearchDocumentBuilder.floorText("7"));
        assertNull(SearchDocumentBuilder.floorText(null));
    }

    @Test
    void placeWithoutKindStillBuildsDocument() {
        Place place = mock(Place.class);
        when(place.getPlaceId()).thenReturn("f-test");
        when(place.getName()).thenReturn("신한은행 ATM");
        when(place.getBuilding()).thenReturn("청운관");
        when(place.getFloorLabel()).thenReturn("L");
        when(place.getArriveNodeId()).thenReturn("CWL_BACK");
        PlaceRepository placeRepository = mock(PlaceRepository.class);
        when(placeRepository.findAll()).thenReturn(List.of(place));
        SearchDocumentRepository searchDocumentRepository = mock(SearchDocumentRepository.class);
        when(searchDocumentRepository.findServiceNamesByPlace()).thenReturn(Map.of());
        SearchDocumentBuilder builder = new SearchDocumentBuilder(placeRepository, mock(NodeRepository.class),
                mock(EdgeRepository.class), searchDocumentRepository);

        List<SearchDocument> documents = builder.buildPlaces();

        assertEquals(1, documents.size());
        assertFalse(documents.get(0).content().contains("종류"));
        assertTrue(documents.get(0).content().contains("신한은행 ATM"));
    }
}
