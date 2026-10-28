package com.smap.domain.search.model;

public record ServicePlace(String serviceId, String serviceName, String placeId, String nodeId, String name,
                           String displaySub, String building, String floorLabel, String kind) {
}
