package com.smap.domain.route.model;

/** 한 번의 탐색에 적용하는 단일 조건이며 기본값은 야외경로 우선이다. */
public enum RouteOption {
    OUTDOOR_FIRST,
    COMFORTABLE,
    SHORTEST,
    INDOOR_UNDERGROUND_FIRST
}
