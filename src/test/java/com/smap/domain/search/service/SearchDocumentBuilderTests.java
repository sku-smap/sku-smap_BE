package com.smap.domain.search.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
}
