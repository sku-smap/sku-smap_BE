package com.smap.domain.search.service;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.model.RouteQuery;
import com.smap.domain.search.model.RouteSearchMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteQuerySplitterTests {
    private final RouteQuerySplitter splitter = new RouteQuerySplitter(new QueryParser());

    private void assertRoute(String query, String origin, String destination) {
        RouteQuery route = splitter.split(query);
        assertEquals(origin, route.origin(), query);
        assertEquals(destination, route.destination(), query);
    }

    @Test
    void splitsFromSentence() {
        assertRoute("북악관에서 청운관 어떻게 가?", "북악관", "청운관");
        assertEquals(RouteSearchMode.ROUTE, splitter.split("북악관에서 청운관 어떻게 가?").mode());
    }

    @Test
    void splitsGluedSttText() {
        assertRoute("북악관에서청운관어떻게가", "북악관", "청운관");
    }

    @Test
    void splitsFromToSentence() {
        assertRoute("북악관부터 청운관까지", "북악관", "청운관");
        assertRoute("북악관부터 청운관까지 가는 길 알려줘", "북악관", "청운관");
    }

    @Test
    void splitsShortSeoAfterPlaceWord() {
        assertRoute("북악관서 청운관 가는 길", "북악관", "청운관");
        assertRoute("1층 엘베 앞서 복사실 어떻게 가", "1층 엘베 앞", "복사실");
    }

    @Test
    void keepsWordsEndingWithSeo() {
        assertRoute("증명서 떼러 어떻게 가", null, "증명서 떼러");
        assertRoute("류일서 교수연구실 어떻게 가", null, "류일서 교수연구실");
    }

    @Test
    void splitsStayingSentence() {
        assertRoute("나 지금 북악관인데 청운관 어떻게 가", "나 지금 북악관", "청운관");
        assertRoute("카페 앞에 있는데 도서관 가고 싶어", "카페 앞에", "도서관");
        assertRoute("나 지금 카페 앞이야 복사실 가고 싶어", "나 지금 카페 앞", "복사실");
    }

    @Test
    void keepsGayaAsMoveIntent() {
        assertRoute("청운관 가야 돼", null, "청운관");
    }

    @Test
    void splitsReversedSentence() {
        assertRoute("청운관 가려면 북악관에서 어떻게 가", "북악관", "청운관");
    }

    @Test
    void treatsHereAsUnknownOrigin() {
        assertRoute("여기서 청운관 어떻게 가", null, "청운관");
        assertRoute("지금 있는 데서 도서관 가는 길", null, "도서관");
        assertEquals(RouteSearchMode.DESTINATION_ONLY, splitter.split("여기서 청운관 어떻게 가").mode());
    }

    @Test
    void returnsDestinationOnlyWithoutOrigin() {
        assertRoute("청운관 어떻게 가", null, "청운관");
        assertRoute("복사실로 가는 길 알려줘", null, "복사실");
    }

    @Test
    void returnsOriginOnlyWhenDestinationMissing() {
        RouteQuery route = splitter.split("북악관에서 어떻게 가");
        assertEquals("북악관", route.origin());
        assertNull(route.destination());
        assertEquals(RouteSearchMode.ORIGIN_ONLY, route.mode());
    }

    @Test
    void keepsPlainQueryAsDestination() {
        assertRoute("청운관", null, "청운관");
        assertRoute("프린트 하고 싶어", null, "프린트 하고 싶어");
    }

    @Test
    void keepsActionSentenceWithPlaceTogether() {
        assertRoute("도서관에서 프린트 하고 싶어", null, "도서관에서 프린트 하고 싶어");
    }

    @Test
    void movesRouteOptionOutOfOrigin() {
        RouteQuery route = splitter.split("계단 없이 북악관에서 청운관 가는 길");
        assertEquals("북악관", route.origin());
        assertEquals("청운관", route.destination());
        assertEquals(RouteOption.COMFORTABLE, route.routeOption());
    }

    @Test
    void neverReturnsEmptyDestination() {
        RouteQuery route = splitter.split("어떻게 가");
        assertNull(route.origin());
        assertEquals("어떻게 가", route.destination());
    }
}
