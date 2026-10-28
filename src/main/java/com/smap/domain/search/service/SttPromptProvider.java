package com.smap.domain.search.service;

import com.smap.domain.place.entity.Place;
import com.smap.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SttPromptProvider {

    private static final int MAX_LENGTH = 400;
    private static final Set<String> PROMPT_KINDS = Set.of("building", "office", "facility");

    private final PlaceRepository placeRepository;
    private volatile String prompt;

    public String prompt() {
        if (prompt == null) {
            prompt = build();
        }
        return prompt;
    }

    public void refresh() {
        prompt = build();
    }

    private String build() {
        String joined = placeRepository.findAll().stream()
                .filter(place -> place.getKind() != null && PROMPT_KINDS.contains(place.getKind()))
                .flatMap(place -> Stream.of(place.getBuilding(), cleanName(place)))
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(name -> !name.isEmpty() && !"야외".equals(name))
                .distinct()
                .collect(Collectors.joining(", "));
        return joined.length() > MAX_LENGTH ? joined.substring(0, joined.lastIndexOf(", ", MAX_LENGTH)) : joined;
    }

    private static String cleanName(Place place) {
        return place.getName().replaceAll("\\s*\\([^)]*\\)", "");
    }
}
