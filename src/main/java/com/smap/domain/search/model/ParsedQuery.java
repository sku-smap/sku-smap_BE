package com.smap.domain.search.model;

import com.smap.domain.route.model.RouteOption;

public record ParsedQuery(String original, String normalized, String core, RouteOption routeOption) {
}
