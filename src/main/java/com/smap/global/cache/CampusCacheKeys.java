package com.smap.global.cache;

/** 응답 구조 변경 시 버전을 올려 이전 JSON과 충돌하지 않게 한다. */
public final class CampusCacheKeys {
    public static final String MAP = "smap:campus:v5:map";
    public static final String NODES = "smap:campus:v4:nodes";
    public static final String PLACES = "smap:campus:v5:places";
    private CampusCacheKeys() {}
}
