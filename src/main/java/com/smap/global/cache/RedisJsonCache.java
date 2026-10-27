package com.smap.global.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** 지정된 DTO만 JSON으로 캐시하며 Redis 장애는 DB 조회로 대체한다. */
@Slf4j
@Component
public class RedisJsonCache {
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper;
    private final CampusCacheProperties properties;

    public RedisJsonCache(@Qualifier("redisTemplate") RedisTemplate<String, String> redis,
                          ObjectMapper mapper, CampusCacheProperties properties) {
        this.redis = redis;
        this.mapper = mapper;
        this.properties = properties;
    }

    /** 캐시 적중 시 로더를 호출하지 않으며 DB·서명 오류 결과는 캐시하지 않는다. */
    public <T> T getOrLoad(String key, TypeReference<T> type, Supplier<T> loader) {
        if (!properties.isEnabled()) return loader.get();
        boolean writable = true;
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                T cached = mapper.readValue(json, type);
                if (cached != null) return cached;
            }
        } catch (DataAccessException exception) {
            // 이미 장애가 확인됐으면 같은 요청에서 다시 쓰기를 시도하지 않는다.
            writable = false;
            log.warn("캠퍼스 캐시 읽기 실패, DB 조회 사용: {}", key);
        } catch (JsonProcessingException exception) {
            log.warn("캠퍼스 캐시 형식 불일치, 데이터 재생성: {}", key);
        }
        T result = loader.get();
        if (writable) {
            try {
                redis.opsForValue().set(key, mapper.writeValueAsString(result),
                        Duration.ofSeconds(properties.getTtlSeconds()));
            } catch (DataAccessException | JsonProcessingException exception) {
                log.warn("캠퍼스 캐시 저장 실패, 조회 결과 반환: {}", key);
            }
        }
        return result;
    }
}
