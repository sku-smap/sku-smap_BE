package com.smap.domain.graph.service;

import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smap.global.cache.CampusCacheKeys;
import com.smap.global.cache.RedisJsonCache;
import com.smap.global.external.s3.PhotoUrlService;
import com.smap.global.external.s3.ThumbnailUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 지도 데이터를 캐시에서 읽고 매 요청마다 유효한 사진 URL을 발급한다. */
@Service
@RequiredArgsConstructor
public class CampusMapService {
    private final CampusMapDataLoader dataLoader;
    private final RedisJsonCache cache;
    private final PhotoUrlService photoUrlService;
    private final ThumbnailUrlService thumbnailUrlService;

    /** 캐시 적중 시 DB 트랜잭션을 시작하지 않는다. */
    public CampusMapResponseDTO load() {
        CampusMapResponseDTO map = cache.getOrLoad(CampusCacheKeys.MAP,
                new TypeReference<CampusMapResponseDTO>() {}, dataLoader::load);
        return map.withImageUrls(photoUrlService::createUrl, thumbnailUrlService::createUrl);
    }
}
