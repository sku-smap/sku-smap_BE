package com.smap.domain.place;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smap.domain.place.dto.response.PlaceAliases;
import com.smap.domain.place.entity.PlaceKind;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 장소 필드의 공백·중복·누락 처리와 enum의 실제 JSON 값을 검증한다. */
class PlaceResponseFieldsTests {
    @Test
    void aliasesKeepOrderAndDoNotSplitCommas() {
        assertEquals(List.of("실습실", "202호", "A, B"), PlaceAliases.parse(" | 실습실 | 202호 || 실습실 | A, B | "));
        assertEquals(List.of(), PlaceAliases.parse(null));
        assertEquals(List.of(), PlaceAliases.parse("   "));
        assertEquals(List.of(), PlaceAliases.parse(" | | "));
    }

    @Test
    void kindsRoundTripAsLowercaseValues() throws Exception {
        var mapper = new ObjectMapper();
        for (PlaceKind kind : PlaceKind.values()) {
            String json = "\"" + kind.value() + "\"";
            assertEquals(json, mapper.writeValueAsString(kind));
            assertEquals(kind, mapper.readValue(json, PlaceKind.class));
        }
        assertNull(PlaceKind.fromValue(null));
        assertNull(PlaceKind.fromValue(" "));
        assertThrows(IllegalArgumentException.class, () -> PlaceKind.fromValue("invalid"));
    }
}
