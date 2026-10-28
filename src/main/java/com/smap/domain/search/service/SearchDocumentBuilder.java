package com.smap.domain.search.service;

import com.smap.domain.graph.entity.Edge;
import com.smap.domain.graph.entity.Node;
import com.smap.domain.graph.repository.EdgeRepository;
import com.smap.domain.graph.repository.NodeRepository;
import com.smap.domain.place.entity.Place;
import com.smap.domain.place.repository.PlaceRepository;
import com.smap.domain.search.model.DocType;
import com.smap.domain.search.model.SearchDocument;
import com.smap.domain.search.repository.SearchDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SearchDocumentBuilder {

    private static final Map<String, String> KIND_NAMES = Map.of(
            "room", "강의실·호실",
            "professor", "교수 연구실",
            "facility", "시설",
            "office", "사무실",
            "building", "건물");

    private static final Map<String, String> STRUCTURES = Map.of(
            "엘베", "엘리베이터 앞",
            "엘리베이터", "엘리베이터 앞",
            "계단", "계단",
            "입구", "입구",
            "출입", "입구",
            "복도", "복도",
            "로비", "로비",
            "갈림길", "갈림길",
            "갈래", "갈림길");

    private static final Pattern PHOTO_EXTENSION = Pattern.compile("(?i)\\.(jpe?g|png)$");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(_\\d+|(?<=층)\\d+)$");
    private static final Pattern STRUCTURE_SPACING = Pattern.compile("(엘베|엘리베이터|계단|입구|복도)(앞|옆|밑|위|안)");
    private static final Pattern FLOOR_ONLY = Pattern.compile("^(지하\\s?)?\\d+층$|^L층$");
    private static final Pattern EDGE_TARGET = Pattern.compile(
            "^(?:.*?(?:통해|따라|타고)\\s+)?(.+?)(?:으로|로)\\s*(?:이동|올라|내려)");

    private final PlaceRepository placeRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final SearchDocumentRepository searchDocumentRepository;

    public List<SearchDocument> buildPlaces() {
        Map<String, List<String>> servicesByPlace = searchDocumentRepository.findServiceNamesByPlace();
        return placeRepository.findAll().stream()
                .filter(place -> place.getArriveNodeId() != null)
                .map(place -> place(place, servicesByPlace.getOrDefault(place.getPlaceId(), List.of())))
                .toList();
    }

    public List<SearchDocument> buildNodes() {
        Map<String, List<Place>> placesByNode = placeRepository.findAll().stream()
                .filter(place -> place.getArriveNodeId() != null)
                .collect(Collectors.groupingBy(Place::getArriveNodeId));
        Map<String, Set<String>> edgeNames = edgeRepository.findAll().stream()
                .collect(Collectors.groupingBy(Edge::getToNodeId,
                        Collectors.mapping(edge -> edgeTarget(edge.getDescription()),
                                Collectors.filtering(Objects::nonNull,
                                        Collectors.toCollection(LinkedHashSet::new)))));
        return nodeRepository.findAll().stream()
                .map(node -> node(node, placesByNode.getOrDefault(node.getNodeId(), List.of()),
                        edgeNames.getOrDefault(node.getNodeId(), Set.of())))
                .toList();
    }

    public List<SearchDocument> buildServices() {
        return searchDocumentRepository.findServices().stream()
                .map(row -> {
                    String name = (String) row.get("name");
                    String description = (String) row.get("description");
                    String keywords = (String) row.get("keywords");
                    String content = join(". ", name, description,
                            keywords == null || keywords.isBlank() ? null : "관련 말: " + keywords.replace(" | ", ", "));
                    return new SearchDocument(DocType.SERVICE, (String) row.get("service_id"), null, name,
                            description, null, null, null, keywordString(Stream.concat(Stream.of(name),
                            splitKeywords(keywords))), content);
                })
                .toList();
    }

    private SearchDocument place(Place place, List<String> services) {
        String location = join(" ", place.getBuilding(), floorText(place.getFloorLabel()),
                place.getRoomNo() == null || place.getRoomNo().isBlank() ? null : place.getRoomNo() + "호");
        String content = join(". ",
                place.getName(),
                location,
                "종류: " + KIND_NAMES.getOrDefault(place.getKind(), place.getKind()),
                place.getAliases() == null ? null : "다른 이름: " + place.getAliases().replace(" | ", ", "),
                services.isEmpty() ? null : "할 수 있는 일: " + String.join(", ", services));
        String buildingRoom = place.getRoomNo() == null || place.getRoomNo().isBlank() || place.getBuilding() == null
                ? null
                : place.getBuilding() + " " + place.getRoomNo() + "호";
        String keywords = keywordString(Stream.concat(
                Stream.of(place.getName(), place.getSub(), location, buildingRoom),
                splitKeywords(place.getAliases())));
        return new SearchDocument(DocType.PLACE, place.getPlaceId(), place.getArriveNodeId(), place.getName(),
                place.getSub() == null ? location : place.getSub(), place.getBuilding(), place.getFloorLabel(),
                place.getKind(), keywords, content);
    }

    private SearchDocument node(Node node, List<Place> places, Set<String> edgeNames) {
        String title = nodeTitle(node.getOriginalFilename());
        String location = join(" ", "야외".equals(node.getBuilding()) ? "야외" : node.getBuilding(),
                floorText(node.getFloorLabel()));
        String structures = STRUCTURES.entrySet().stream()
                .filter(entry -> title.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        List<String> nearbyNames = places.stream()
                .filter(place -> "facility".equals(place.getKind()) || "office".equals(place.getKind())
                        || "building".equals(place.getKind()))
                .map(Place::getName)
                .distinct()
                .limit(8)
                .toList();
        List<String> nearbyCounts = places.stream()
                .filter(place -> "professor".equals(place.getKind()) || "room".equals(place.getKind()))
                .collect(Collectors.groupingBy(place -> KIND_NAMES.get(place.getKind()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue() + "곳")
                .toList();
        List<String> nearby = new ArrayList<>(nearbyNames);
        nearby.addAll(nearbyCounts);
        List<String> expressions = edgeNames.stream().filter(name -> !name.equals(title)).limit(3).toList();
        String content = join(". ",
                title,
                location,
                structures.isEmpty() ? null : "구조물: " + structures,
                nearby.isEmpty() ? null : "주변: " + String.join(", ", nearby),
                expressions.isEmpty() ? null : "안내 표현: " + String.join(", ", expressions));
        String keywords = keywordString(Stream.of(
                Stream.of(title, title.replace("엘베", "엘리베이터"), location),
                expressions.stream(),
                nearbyNames.stream()).flatMap(s -> s));
        return new SearchDocument(DocType.NODE, node.getNodeId(), node.getNodeId(), title, location,
                node.getBuilding(), node.getFloorLabel(), null, keywords, content);
    }

    static String nodeTitle(String filename) {
        String title = PHOTO_EXTENSION.matcher(filename).replaceFirst("");
        title = TRAILING_NUMBER.matcher(title).replaceFirst("");
        title = STRUCTURE_SPACING.matcher(title.replace('_', ' ')).replaceAll("$1 $2");
        return title.replaceAll("\\s+", " ").strip();
    }

    static String edgeTarget(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        Matcher matcher = EDGE_TARGET.matcher(description.strip());
        if (!matcher.find()) {
            return null;
        }
        String target = STRUCTURE_SPACING.matcher(matcher.group(1).strip()).replaceAll("$1 $2");
        return FLOOR_ONLY.matcher(target).matches() ? null : target;
    }

    static String floorText(String floorLabel) {
        if (floorLabel == null || floorLabel.isBlank()) {
            return null;
        }
        if (floorLabel.startsWith("B")) {
            return "지하" + floorLabel.substring(1) + "층";
        }
        return floorLabel + "층";
    }

    private static Stream<String> splitKeywords(String value) {
        return value == null ? Stream.empty() : Arrays.stream(value.split("\\|"));
    }

    private static String keywordString(Stream<String> values) {
        return values.filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining(" | "));
    }

    private static String join(String delimiter, String... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(delimiter));
    }
}
