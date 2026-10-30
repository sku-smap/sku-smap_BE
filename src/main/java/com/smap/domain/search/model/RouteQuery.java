package com.smap.domain.search.model;

import com.smap.domain.route.model.RouteOption;

public record RouteQuery(String origin, String destination, RouteOption routeOption) {

    public RouteSearchMode mode() {
        if (origin != null && destination != null) {
            return RouteSearchMode.ROUTE;
        }
        return origin != null ? RouteSearchMode.ORIGIN_ONLY : RouteSearchMode.DESTINATION_ONLY;
    }
}
