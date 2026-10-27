package com.smap.domain.place.service;

import com.smap.domain.place.dto.response.PlaceResponseDTO;
import com.smap.domain.place.repository.PlaceRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smap.global.cache.CampusCacheKeys;
import com.smap.global.cache.RedisJsonCache;

/** 노드에 연결된 장소를 이름·별명·건물·호실로 조회한다. */
@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final RedisJsonCache cache;

    /** 검색어 생략·공백은 전체 조회이며 앞뒤 공백과 영문 대소문자를 정규화한다. */
    public List<PlaceResponseDTO> search(String keyword) {
        String normalized = keyword == null ? "" : Normalizer.normalize(keyword.strip(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return cache.getOrLoad(CampusCacheKeys.PLACES, new TypeReference<List<PlaceResponseDTO>>() {},
                    () -> placeRepository.searchConnected("").stream().map(PlaceResponseDTO::from).toList());
        }
        return placeRepository.searchConnected(normalized).stream().map(PlaceResponseDTO::from).toList();
    }
}
