package com.smap.domain.search.service;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.model.ParsedQuery;
import com.smap.domain.search.model.SearchTarget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryParserTests {
    private final QueryParser parser = new QueryParser();

    @Test
    void stripsEndingForOrigin() {
        ParsedQuery parsed = parser.parse("북악관 7층 엘리베이터 앞이야", SearchTarget.ORIGIN);
        assertEquals("북악관 7층 엘리베이터 앞", parsed.normalized());
        assertEquals("북악관 7층 엘리베이터 앞이야", parsed.original());
        assertNull(parsed.routeOption());
    }

    @Test
    void extractsComfortableOptionForDestination() {
        ParsedQuery parsed = parser.parse("계단 말고 복사실 가고 싶어", SearchTarget.DESTINATION);
        assertEquals(RouteOption.COMFORTABLE, parsed.routeOption());
        assertEquals("복사실", parsed.normalized());
    }

    @Test
    void ignoresRouteOptionForOrigin() {
        ParsedQuery parsed = parser.parse("계단 말고 복사실", SearchTarget.ORIGIN);
        assertNull(parsed.routeOption());
        assertEquals("계단 말고 복사실", parsed.normalized());
    }

    @Test
    void extractsIndoorOption() {
        ParsedQuery parsed = parser.parse("실내로 도서관 가고 싶어", SearchTarget.DESTINATION);
        assertEquals(RouteOption.INDOOR_UNDERGROUND_FIRST, parsed.routeOption());
        assertEquals("도서관", parsed.normalized());
    }

    @Test
    void extractsShortestOption() {
        ParsedQuery parsed = parser.parse("제일 빠른 길로 북악관", SearchTarget.DESTINATION);
        assertEquals(RouteOption.SHORTEST, parsed.routeOption());
        assertEquals("북악관", parsed.normalized());
    }

    @Test
    void joinsBasementFloor() {
        assertEquals("지하1층 편의점", parser.parse("지하 1 층 편의점 어디야?", SearchTarget.ORIGIN).normalized());
    }

    @Test
    void joinsRoomNumber() {
        assertEquals("북악관 102호", parser.parse("북악관 102 호", SearchTarget.DESTINATION).normalized());
    }

    @Test
    void neverBecomesEmpty() {
        assertEquals("어디야", parser.parse("어디야", SearchTarget.DESTINATION).normalized());
    }

    @Test
    void keepsPlainQueryWithoutOption() {
        ParsedQuery parsed = parser.parse("배고파", SearchTarget.DESTINATION);
        assertEquals("배고파", parsed.normalized());
        assertNull(parsed.routeOption());
    }

    @Test
    void dropsFillersAndRelationWords() {
        ParsedQuery parsed = parser.parse("나 지금 카페 앞이야", SearchTarget.ORIGIN);
        assertEquals("카페 앞", parsed.normalized());
        assertEquals("카페", parsed.core());
    }

    @Test
    void dropsGluedRelationWord() {
        assertEquals("북악관 지하1층 엘베", parser.parse("북악관 지하1층 엘베앞이야", SearchTarget.ORIGIN).core());
    }

    @Test
    void keepsTextWhenOnlyFillersRemain() {
        ParsedQuery parsed = parser.parse("여기 앞", SearchTarget.ORIGIN);
        assertEquals("앞", parsed.normalized());
        assertEquals("앞", parsed.core());
    }
}
