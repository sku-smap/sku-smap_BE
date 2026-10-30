package com.smap.domain.search.service;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.model.ParsedQuery;
import com.smap.domain.search.model.SearchTarget;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class QueryParser {

    private static final Map<RouteOption, Pattern> ROUTE_OPTIONS = new LinkedHashMap<>();

    static {
        ROUTE_OPTIONS.put(RouteOption.COMFORTABLE,
                Pattern.compile("계단\\s*(말고|없이|없는|안\\s*쓰고|피해서)|엘리베이터로|엘베로|편한\\s*길로?|편하게"));
        ROUTE_OPTIONS.put(RouteOption.INDOOR_UNDERGROUND_FIRST,
                Pattern.compile("실내로|건물\\s*안으로|비\\s*안\\s*맞게|지하로"));
        ROUTE_OPTIONS.put(RouteOption.SHORTEST,
                Pattern.compile("(제일|가장)\\s*(짧은|빠른)\\s*(길로?)?|최단\\s*(거리|경로)?로?|빨리"));
    }

    private static final Pattern ENDING = Pattern.compile(
            "\\s*(으로|로|에)?\\s*(가고\\s*싶어요?|가고\\s*싶은데|갈래요?|가려고요?|가자|가야\\s*돼요?|"
                    + "어디\\s*(야|예요|에요|있어요?|있나요?|인가요?|지)?|찾아\\s*줘요?|알려\\s*줘요?|"
                    + "이에요|예요|에요|입니다|이야|야|요)?\\s*[?.!~]*$");

    private static final Set<String> FILLERS = Set.of(
            "나", "난", "저", "전", "나는", "저는", "내가", "제가", "우리", "우리는",
            "지금", "지금은", "현재", "여기", "여기는", "거기", "혹시", "그냥", "좀", "막");
    private static final Set<String> RELATIONS = Set.of(
            "앞", "옆", "뒤", "근처", "쪽", "주변", "건너편", "맞은편", "안", "밖", "부근", "사이");
    private static final Pattern GLUED_RELATION = Pattern.compile("^(.{2,}?)(앞|옆|뒤|근처|쪽)$");

    private static final Pattern BASEMENT = Pattern.compile("지하\\s+(\\d)\\s*층");
    private static final Pattern FLOOR = Pattern.compile("(\\d+)\\s+층");
    private static final Pattern ROOM = Pattern.compile("(\\d+)\\s+호");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public ParsedQuery parse(String query, SearchTarget target) {
        String text = SPACES.matcher(query.strip()).replaceAll(" ");
        RouteOption routeOption = null;
        if (target == SearchTarget.DESTINATION) {
            routeOption = findRouteOption(text);
            text = removeRouteOption(text);
        }
        text = SPACES.matcher(text).replaceAll(" ").strip();
        String stripped = ENDING.matcher(text).replaceFirst("").strip();
        if (!stripped.isEmpty()) {
            text = stripped;
        }
        text = BASEMENT.matcher(text).replaceAll("지하$1층");
        text = FLOOR.matcher(text).replaceAll("$1층");
        text = ROOM.matcher(text).replaceAll("$1호");
        String normalized = keepIfEmpty(Arrays.stream(text.split(" "))
                .filter(token -> !FILLERS.contains(token))
                .collect(Collectors.joining(" ")), text);
        String core = keepIfEmpty(Arrays.stream(normalized.split(" "))
                .filter(token -> !RELATIONS.contains(token))
                .map(token -> GLUED_RELATION.matcher(token).replaceFirst("$1"))
                .collect(Collectors.joining(" ")), normalized);
        return new ParsedQuery(query, normalized, core, routeOption);
    }

    public RouteOption findRouteOption(String text) {
        for (Map.Entry<RouteOption, Pattern> entry : ROUTE_OPTIONS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String removeRouteOption(String text) {
        RouteOption routeOption = findRouteOption(text);
        return routeOption == null ? text : ROUTE_OPTIONS.get(routeOption).matcher(text).replaceAll(" ");
    }

    private static String keepIfEmpty(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }
}
