package com.smap.domain.search.service;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.model.RouteQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RouteQuerySplitter {

    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("[?.!~]+");

    private static final String MOVE = "어떻게\\s*(가|갈)|가는\\s*(길|법|방법)|가려면|까지|가야|가고\\s*싶|갈래|가자|데려다|길\\s*(좀\\s*)?(알려|찾아|안내)";
    private static final Pattern INTENT = Pattern.compile(MOVE);
    private static final Pattern ACTION = Pattern.compile("하고\\s*싶|하려|하러|할래|싶어|싶은|해야|할\\s*수");
    private static final Pattern ENDING = Pattern.compile(
            "\\s*(으로|로|에|까지)?\\s*((" + MOVE + "|어떻게|어디로).*)?$");

    private static final Pattern REVERSED = Pattern.compile(
            "^(?<destination>.+?)\\s*(으로|로|에)?\\s*가려면\\s+(?<origin>.+?)(에서|부터)\\s*.*$");
    private static final Pattern FROM = Pattern.compile(
            "^(?<origin>.+?)(에서|부터|(?<=[관층실호문앞옆뒤쪽역장당소터데]|여기|거기|저기)서)\\s*(?<destination>.+)$");
    private static final Pattern STAYING = Pattern.compile(
            "^(?<origin>.+?)(이?(인데요?|거든요?)|이야|이에요|예요|있는데요?|있어요?)\\s+(?<destination>.+)$");
    private static final Pattern HERE = Pattern.compile(
            "^((나|난|저|전|제가|내가|우리)\\s+)?((지금|현재)\\s*)?(여기|거기|저기|이곳|이쪽|위치|내\\s*위치|제\\s*위치|있는\\s*(곳|데))$");

    private final QueryParser queryParser;

    public RouteQuery split(String query) {
        String text = SPACES.matcher(PUNCTUATION.matcher(query).replaceAll(" ")).replaceAll(" ").strip();
        RouteOption routeOption = queryParser.findRouteOption(text);
        text = SPACES.matcher(queryParser.removeRouteOption(text)).replaceAll(" ").strip();
        boolean intent = INTENT.matcher(text).find();

        Matcher reversed = REVERSED.matcher(text);
        if (reversed.matches()) {
            return build(reversed.group("origin"), reversed.group("destination"), text, routeOption);
        }
        for (Pattern pattern : List.of(FROM, STAYING)) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches() && (intent || !ACTION.matcher(matcher.group("destination")).find())) {
                return build(matcher.group("origin"), matcher.group("destination"), text, routeOption);
            }
        }
        if (!intent) {
            return new RouteQuery(null, text, routeOption);
        }
        return build(null, text, text, routeOption);
    }

    private RouteQuery build(String origin, String destination, String text, RouteOption routeOption) {
        String from = origin == null || HERE.matcher(origin.strip()).matches() ? null : origin.strip();
        String to = ENDING.matcher(destination).replaceFirst("").strip();
        if (to.isEmpty()) {
            return from == null ? new RouteQuery(null, text, routeOption) : new RouteQuery(from, null, routeOption);
        }
        return new RouteQuery(from, to, routeOption);
    }
}
