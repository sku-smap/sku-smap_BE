package com.smap.domain.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.smap.domain.graph.entity.Node;
import com.smap.domain.graph.repository.NodeRepository;
import com.smap.domain.graph.service.CampusMapDataLoader;
import com.smap.domain.graph.service.CampusMapService;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.graph.service.NodeService;
import com.smap.domain.place.repository.PlaceRepository;
import com.smap.domain.place.service.PlaceService;
import com.smap.global.cache.CampusCacheProperties;
import com.smap.global.cache.RedisJsonCache;
import com.smap.global.external.s3.PhotoUrlService;
import com.smap.global.external.s3.ThumbnailUrlService;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 서비스 재조회가 실제 DB 로더를 건너뛰며 사진 URL만 새로 발급하는지 검증한다. */
class CampusCacheServiceTests {
    @SuppressWarnings("unchecked")
    private RedisJsonCache cache() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        Map<String, String> data = new HashMap<>();
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> data.get(call.getArgument(0)));
        doAnswer(call -> { data.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), any(Duration.class));
        return new RedisJsonCache(redis, new ObjectMapper(), new CampusCacheProperties());
    }

    @Test
    void mapLoadsDatabaseOnceButRenewsUrlsOnBothRequests() {
        var loader = mock(CampusMapDataLoader.class);
        var photos = mock(PhotoUrlService.class);
        var thumbnails = mock(ThumbnailUrlService.class);
        var graph = new GraphSnapshot(Map.of("a", new GraphSnapshot.Point("a", "본관", "1", "본관", "a.jpg", "BON")), Map.of());
        when(loader.load()).thenReturn(CampusMapResponseDTO.from(graph, List.of(), filename -> null));
        when(photos.createUrl("a.jpg")).thenReturn("first", "second");
        when(thumbnails.createUrl("a.jpg")).thenReturn("thumb-first", "thumb-second");
        var service = new CampusMapService(loader, cache(), photos, thumbnails);
        var firstNode = service.load().nodes().get(0);
        assertEquals("first", firstNode.imageUrl());
        assertEquals("thumb-first", firstNode.thumbnailUrl());
        var cachedNode = service.load().nodes().get(0);
        assertEquals("second", cachedNode.imageUrl());
        assertEquals("thumb-second", cachedNode.thumbnailUrl());
        assertEquals(com.smap.domain.graph.entity.PhotoType.PANORAMA, cachedNode.photoType());
        assertEquals("BON", cachedNode.buildingId());
        verify(loader).load();
        verify(photos, times(2)).createUrl("a.jpg");
    }

    @Test
    void allNodesAndPlacesAreCachedButKeywordSearchStillQueriesDatabase() {
        var nodes = mock(NodeRepository.class);
        var node = mock(Node.class);
        when(node.getNodeId()).thenReturn("a");
        when(node.getOriginalFilename()).thenReturn("a.jpg");
        when(nodes.findAll(any(Sort.class))).thenReturn(List.of(node));
        var places = mock(PlaceRepository.class);
        when(places.searchConnected(anyString())).thenReturn(List.of());
        var shared = cache();
        var nodeService = new NodeService(nodes, shared);
        var placeService = new PlaceService(places, shared);
        assertEquals(nodeService.findAll(), nodeService.findAll());
        placeService.search(null);
        placeService.search("   ");
        placeService.search("본관");
        verify(nodes).findAll(any(Sort.class));
        verify(places).searchConnected("");
        verify(places).searchConnected("본관");
    }
}
