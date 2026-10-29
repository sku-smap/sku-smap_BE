package com.smap.global.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.smap.domain.graph.dto.response.NodeResponseDTO;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.place.dto.response.PlaceResponseDTO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/** 다른 프로젝트 Redis와 운영 키를 건드리지 않고 SMAP Redis의 실제 적중·TTL을 확인한다. */
@SpringBootTest(properties = {"spring.data.redis.port=6380", "campus.cache.enabled=true", "campus.cache.ttl-seconds=2"})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SMAP_REDIS_TEST", matches = "true")
class CampusCacheIntegrationTests {
    @Autowired RedisJsonCache cache;
    @Autowired @Qualifier("redisTemplate") RedisTemplate<String, String> redis;
    private final String key = "smap:test:cache:" + UUID.randomUUID();

    @AfterEach
    void cleanup() { redis.delete(List.of(key, key + ":nodes", key + ":places")); }

    @Test
    void mapRoundTripsAndRefreshesPhotoUrlsWithoutReloadingData() {
        var graph = new GraphSnapshot(Map.of("a", new GraphSnapshot.Point("a", "본관", "1", "본관", "본관.jpg")), Map.of());
        var data = CampusMapResponseDTO.from(graph, List.of(), filename -> null);
        AtomicInteger loads = new AtomicInteger();
        var type = new TypeReference<CampusMapResponseDTO>() {};
        var first = cache.getOrLoad(key, type, () -> { loads.incrementAndGet(); return data; });
        var second = cache.getOrLoad(key, type, () -> { loads.incrementAndGet(); return data; });
        assertEquals(1, loads.get());
        assertEquals(first, second);
        assertEquals("url-first", first.withImageUrls(filename -> "url-first", filename -> null).nodes().get(0).imageUrl());
        assertEquals("url-second", second.withImageUrls(filename -> "url-second", filename -> null).nodes().get(0).imageUrl());
        String raw = redis.opsForValue().get(key);
        assertNotNull(raw);
        assertFalse(raw.contains("url-first"));
        assertFalse(raw.contains("url-second"));
        assertTrue(raw.contains("\"imageUrl\":null"));
    }

    @Test
    void listTypesRoundTripAndExpiredCacheReloads() {
        var nodes = List.of(new NodeResponseDTO("a", "본관", "1", "본관", "본관.jpg", com.smap.domain.graph.entity.PhotoType.PANORAMA, "BON", List.of()));
        var places = List.of(new PlaceResponseDTO("p", "강의실", com.smap.domain.place.entity.PlaceKind.ROOM, "본관", "1", "101", "a", "본관", List.of(), null, "BON"));
        AtomicInteger nodeLoads = new AtomicInteger();
        AtomicInteger placeLoads = new AtomicInteger();
        var nodeType = new TypeReference<List<NodeResponseDTO>>() {};
        var placeType = new TypeReference<List<PlaceResponseDTO>>() {};
        for (int i = 0; i < 2; i++) {
            assertEquals(nodes, cache.getOrLoad(key + ":nodes", nodeType, () -> { nodeLoads.incrementAndGet(); return nodes; }));
            assertEquals(places, cache.getOrLoad(key + ":places", placeType, () -> { placeLoads.incrementAndGet(); return places; }));
        }
        assertEquals(1, nodeLoads.get());
        assertEquals(1, placeLoads.get());
        Long ttl = redis.getExpire(key + ":nodes");
        assertNotNull(ttl);
        assertTrue(ttl >= 0 && ttl <= 2);
        await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.FALSE.equals(redis.hasKey(key + ":nodes")));
        cache.getOrLoad(key + ":nodes", nodeType, () -> { nodeLoads.incrementAndGet(); return nodes; });
        assertEquals(2, nodeLoads.get());
    }
}
