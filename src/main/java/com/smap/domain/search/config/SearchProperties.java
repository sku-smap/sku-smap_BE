package com.smap.domain.search.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private double semanticWeight = 0.6;
    private double keywordWeight = 0.4;
    private double threshold = 0.45;
    private double keywordHit = 0.5;
    private double semanticHit = 0.4;
    private int poolSize = 50;
    private int embeddingBatchSize = 100;
    private String adminKey;

    public boolean isAdminKey(String key) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(key);
    }
}
