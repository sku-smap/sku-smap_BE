package com.smap.domain.graph.entity;

/** CSV에서 사용하는 이동 유형이며 실내외 구분과는 별개이다. */
public enum MoveType {
    /** 일반 도보 이동. */
    WALK,
    /** 계단을 통한 이동. */
    STAIRS,
    /** 엘리베이터를 통한 이동. */
    ELEVATOR,
    /** 출입문 통과 구간. */
    DOOR
}
