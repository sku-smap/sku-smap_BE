package com.smap.domain.graph.service;

import com.smap.domain.graph.dto.response.BuildingResponseDTO;
import com.smap.domain.graph.repository.BuildingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 건물·구역 식별 목록을 최신 DB에서 읽는다. 사진 URL은 발급하지 않는다. */
@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;

    @Transactional(readOnly = true)
    public List<BuildingResponseDTO> findAll() {
        return buildingRepository.findAll();
    }
}
