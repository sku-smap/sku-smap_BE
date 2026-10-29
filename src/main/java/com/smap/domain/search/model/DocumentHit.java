package com.smap.domain.search.model;

public record DocumentHit(DocType docType, String refId, String nodeId, String displayName, String displaySub,
                          String building, String floorLabel, String kind,
                          double keywordScore, Double semanticScore, double score, String buildingId) {
}
