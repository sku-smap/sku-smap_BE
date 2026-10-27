package com.smap.domain.graph.service;

import com.smap.domain.graph.dto.response.NodeResponseDTO;
import com.smap.domain.graph.repository.NodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smap.global.cache.CampusCacheKeys;
import com.smap.global.cache.RedisJsonCache;

/** 간선·S3 조회 없이 노드 목록만 읽어 초기 선택 화면에 제공한다. */
@Service
@RequiredArgsConstructor
public class NodeService {
    private final NodeRepository nodeRepository;
    private final RedisJsonCache cache;

    /** 같은 데이터에는 같은 순서를 제공하도록 노드 ID 오름차순으로 반환한다. */
    public List<NodeResponseDTO> findAll() {
        return cache.getOrLoad(CampusCacheKeys.NODES, new TypeReference<List<NodeResponseDTO>>() {},
                () -> nodeRepository.findAll(Sort.by("nodeId")).stream().map(NodeResponseDTO::from).toList());
    }
}
