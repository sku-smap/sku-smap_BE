package com.smap.domain.graph.service;

import com.smap.domain.graph.dto.response.NodeResponseDTO;
import java.util.List;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smap.global.cache.CampusCacheKeys;
import com.smap.global.cache.RedisJsonCache;

/** 노드·이동 선택지를 같은 그래프 스냅샷에서 읽고 캐시한다. 사진 URL은 발급하지 않는다. */
@Service
@RequiredArgsConstructor
public class NodeService {
    private final GraphService graphService;
    private final RedisJsonCache cache;

    /** 같은 데이터에는 같은 순서를 제공하도록 노드 ID 오름차순으로 반환한다. */
    public List<NodeResponseDTO> findAll() {
        return cache.getOrLoad(CampusCacheKeys.NODES, new TypeReference<List<NodeResponseDTO>>() {},
                this::loadNodes);
    }

    private List<NodeResponseDTO> loadNodes() {
        var graph = graphService.load();
        return graph.nodes().values().stream().sorted(Comparator.comparing(GraphSnapshot.Point::nodeId))
                .map(node -> NodeResponseDTO.from(node, graph.outgoing().getOrDefault(node.nodeId(), List.of())))
                .toList();
    }
}
