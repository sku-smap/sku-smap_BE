package com.smap.domain.search.model;

public record SearchDocument(DocType docType, String refId, String nodeId, String displayName, String displaySub,
                             String building, String floorLabel, String kind, String keywords, String content) {
}
