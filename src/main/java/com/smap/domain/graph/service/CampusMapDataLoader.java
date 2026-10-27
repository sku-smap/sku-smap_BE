package com.smap.domain.graph.service;

import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.smap.domain.place.dto.response.PlaceResponseDTO;
import com.smap.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 캐시가 없을 때만 하나의 DB 스냅샷에서 지도 데이터를 구성한다. */
@Service
@RequiredArgsConstructor
public class CampusMapDataLoader {
    private final GraphService graphService;
    private final PlaceRepository placeRepository;

    /** 만료되는 사진 URL과 AWS 자격 증명은 Redis에 저장하지 않는다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CampusMapResponseDTO load() {
        var graph = graphService.load();
        var places = placeRepository.searchConnected("").stream().map(PlaceResponseDTO::from).toList();
        return CampusMapResponseDTO.from(graph, places, filename -> null);
    }
}
