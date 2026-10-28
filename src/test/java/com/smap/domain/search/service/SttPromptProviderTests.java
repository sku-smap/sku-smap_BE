package com.smap.domain.search.service;

import com.smap.domain.place.entity.Place;
import com.smap.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SttPromptProviderTests {

    @Test
    void placeWithoutKindIsSkipped() {
        List<Place> places = List.of(place(null, "알 수 없음", "청운관"), place("facility", "복사실 (102호)", "청운관"));
        PlaceRepository placeRepository = mock(PlaceRepository.class);
        when(placeRepository.findAll()).thenReturn(places);

        String prompt = new SttPromptProvider(placeRepository).prompt();

        assertTrue(prompt.contains("복사실"));
        assertTrue(prompt.contains("청운관"));
        assertFalse(prompt.contains("알 수 없음"));
        assertFalse(prompt.contains("(102호)"));
    }

    private static Place place(String kind, String name, String building) {
        Place place = mock(Place.class);
        when(place.getKind()).thenReturn(kind);
        when(place.getName()).thenReturn(name);
        when(place.getBuilding()).thenReturn(building);
        return place;
    }
}
