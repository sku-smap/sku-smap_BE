package com.smap.domain.graph.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/** 현재 노드 사진은 사용자 확인에 따라 모두 360도 파노라마이다. */
@Schema(description = "노드 사진 유형. 현재 모든 노드는 PANORAMA이며 일반 사진 노드는 없다.")
public enum PhotoType {
    PANORAMA
}
