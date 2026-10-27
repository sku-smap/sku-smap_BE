package com.smap.global.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 캐시 적중·장애·손상·TTL 및 DB 오류 전파를 검증한다. */
@SuppressWarnings("unchecked")
class RedisJsonCacheTests {
    RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    Supplier<List<String>> loader = mock(Supplier.class);
    CampusCacheProperties settings = new CampusCacheProperties();
    RedisJsonCache cache = new RedisJsonCache(redis, new ObjectMapper(), settings);
    TypeReference<List<String>> type = new TypeReference<>() {};

    @BeforeEach
    void setup() { when(redis.opsForValue()).thenReturn(values); }

    @Test
    void hitSkipsDatabase() {
        when(values.get("key")).thenReturn("[\"cached\"]");
        assertEquals(List.of("cached"), cache.getOrLoad("key", type, loader));
        verifyNoInteractions(loader);
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void missWritesJsonWithTtlAndCorruptValueIsReplaced() {
        when(values.get("key")).thenReturn("{broken");
        when(loader.get()).thenReturn(List.of("fresh"));
        assertEquals(List.of("fresh"), cache.getOrLoad("key", type, loader));
        verify(values).set("key", "[\"fresh\"]", Duration.ofSeconds(300));
    }

    @Test
    void redisFailureFallsBackWithoutSecondWriteAttempt() {
        when(values.get("key")).thenThrow(new RedisConnectionFailureException("unavailable"));
        when(loader.get()).thenReturn(List.of("db"));
        assertEquals(List.of("db"), cache.getOrLoad("key", type, loader));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void writeFailureStillReturnsDatabaseResult() {
        when(loader.get()).thenReturn(List.of("db"));
        doThrow(new RedisConnectionFailureException("unavailable"))
                .when(values).set(anyString(), anyString(), any(Duration.class));
        assertEquals(List.of("db"), cache.getOrLoad("key", type, loader));
    }

    @Test
    void disabledBypassesRedisAndDatabaseErrorsAreNotCached() {
        settings.setEnabled(false);
        when(loader.get()).thenReturn(List.of());
        assertTrue(cache.getOrLoad("key", type, loader).isEmpty());
        verify(redis, never()).opsForValue();
        settings.setEnabled(true);
        when(loader.get()).thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> cache.getOrLoad("key", type, loader));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
