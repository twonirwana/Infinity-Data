package de.twonirwana.infinity;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JsonDiff {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<JsonNodeType> VALUE_TYPES = EnumSet.of(JsonNodeType.NUMBER, JsonNodeType.STRING, JsonNodeType.BOOLEAN);

    private static void compareNode(JsonNode left, JsonNode right, List<String> path, List<Diff> diffs) {
        if (left != null && right != null) {
            if (!Objects.equals(left.asString(), right.asString())) {
                diffs.add(new Diff(path, left.asString(), right.asString(), DiffType.different_value));
            }
        } else if (left != null) {
            diffs.add(new Diff(path, left.asString(), null, DiffType.missing_right));
        } else if (right != null) {
            diffs.add(new Diff(path, null, right.asString(), DiffType.missing_left));
        }
    }

    private static void recursiveDiffs(JsonNode left, JsonNode right, List<String> path, List<Diff> diffs, List<String> filterPaths, List<String> idKeys) {
        List<JsonNodeType> nodeTypes = Stream.concat(Optional.ofNullable(left).stream(), Optional.ofNullable(right).stream())
                .map(JsonNode::getNodeType)
                .distinct()
                .toList();
        if (nodeTypes.size() != 1) {
            diffs.add(new Diff(path,
                    Optional.ofNullable(left).map(JsonNode::toPrettyString).orElse(""),
                    Optional.ofNullable(right).map(JsonNode::toPrettyString).orElse(""),
                    DiffType.different_type));
        }
        JsonNodeType nodeType = nodeTypes.getFirst();
        if (VALUE_TYPES.contains(nodeType)) {
            compareNode(left, right, path, diffs);
        } else if (nodeType == JsonNodeType.OBJECT) {
            Map<String, JsonNode> leftProperties = Optional.ofNullable(left)
                    .map(n -> n.properties().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .orElse(Map.of());
            Map<String, JsonNode> rightProperties = Optional.ofNullable(right)
                    .map(n -> n.properties().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .orElse(Map.of());
            compareMaps(leftProperties, rightProperties, path, diffs, filterPaths, idKeys);
        } else if (nodeType == JsonNodeType.ARRAY) {
            List<JsonNode> leftValues = Optional.ofNullable(left).map(n -> n.values().stream().toList()).orElse(List.of());
            List<JsonNode> rightValues = Optional.ofNullable(right).map(n -> n.values().stream().toList()).orElse(List.of());
            boolean hasKey = compareListsWithPossibleIdKeys(leftValues, rightValues, path, diffs, filterPaths, idKeys);
            if (!hasKey) {
                compareMaps(toOrderIdMap(leftValues), toOrderIdMap(rightValues), path, diffs, filterPaths, idKeys);
            }
        }
    }

    private static boolean compareListsWithPossibleIdKeys(List<JsonNode> leftValues, List<JsonNode> rightValues, List<String> path, List<Diff> diffs, List<String> filterPaths, List<String> idKeys) {
        for (String key : idKeys) {
            if (hasUniqueIds(leftValues, key) && hasUniqueIds(rightValues, key)) {
                compareListsWithIdKey(leftValues, rightValues, path, diffs, filterPaths, idKeys, key);
                return true;
            }
        }
        return false;
    }

    private static void compareListsWithIdKey(List<JsonNode> leftValues,
                                              List<JsonNode> rightValues,
                                              List<String> path,
                                              List<Diff> diffs,
                                              List<String> filterPaths,
                                              List<String> idKeys,
                                              String currentIdKey) {
        Map<String, JsonNode> leftIdMap = leftValues.stream()
                .collect(Collectors.toMap(n -> n.get(currentIdKey).asString(), n -> n));
        Map<String, JsonNode> rightIdMap = rightValues.stream()
                .collect(Collectors.toMap(n -> n.get(currentIdKey).asString(), n -> n));
        compareMaps(leftIdMap, rightIdMap, path, diffs, filterPaths, idKeys);
    }

    private static Map<String, JsonNode> toOrderIdMap(List<JsonNode> nodes) {
        Map<String, JsonNode> orderIdMap = new HashMap<>();
        //by using the value with a counter there is a better output for inserts into a list, where otherwise all ids of after the insert would change
        if (nodes.stream().allMatch(n -> VALUE_TYPES.contains(n.getNodeType()))) {
            Map<String, Integer> idCounter = new HashMap<>();
            for (JsonNode node : nodes) {
                String value = node.asString();
                int keyCounter = idCounter.getOrDefault(value, 0);
                orderIdMap.put(value + "_" + keyCounter, node);
                idCounter.put(value, keyCounter + 1);
            }
        } else {
            int counter = 0;
            for (JsonNode node : nodes) {
                orderIdMap.put(counter + "", node);
                counter++;
            }
        }
        return orderIdMap;
    }

    private static boolean hasUniqueIds(List<JsonNode> elements, String idKey) {
        if (elements == null || elements.isEmpty()) {
            return true;
        }
        List<String> ids = elements.stream()
                .map(n -> n.get(idKey))
                .map(n -> Optional.ofNullable(n).map(JsonNode::asString).orElse(""))
                .toList();
        return ids.stream().noneMatch(String::isEmpty) && ids.size() == ids.stream().distinct().count();
    }

    private static boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }

    private static void compareMaps(Map<String, JsonNode> left, Map<String, JsonNode> right, List<String> path, List<Diff> diffs, List<String> filterPaths, List<String> idKeys) {
        List<String> bothKeys = Stream.concat(left.keySet().stream(), right.keySet().stream())
                .filter(k -> !filterPaths.contains(k))
                .distinct()
                .sorted((s1, s2) -> {
                    boolean n1 = isNumeric(s1);
                    boolean n2 = isNumeric(s2);

                    if (n1 && n2) {
                        return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
                    } else if (n1) {
                        return -1; // Numbers first
                    } else if (n2) {
                        return 1;  // Numbers first
                    } else {
                        return s1.compareTo(s2);
                    }
                }).toList();
        for (String key : bothKeys) {
            List<String> newPath = new ArrayList<>(path);
            newPath.add(key);
            recursiveDiffs(left.get(key), right.get(key), newPath, diffs, filterPaths, idKeys);
        }
    }

    public static List<Diff> getDiffs(String left, String right, List<String> filterPaths) {
        try {
            JsonNode json1Node = OBJECT_MAPPER.readTree(left);
            JsonNode json2Node = OBJECT_MAPPER.readTree(right);

            List<Diff> diffs = new ArrayList<>();
            recursiveDiffs(json1Node, json2Node, List.of(), diffs, filterPaths, List.of("id", "order"));
            return diffs;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return List.of();
        }
    }

    public enum DiffType {
        missing_left,
        missing_right,
        different_type,
        different_value
    }

    private record UniqueKey(String key, int counter) {
    }

    public record Diff(List<String> path, String valueLeft, String valueRight, DiffType diffType) {
        @Override
        public @NonNull String toString() {
            return "%s: '%s'->'%s'".formatted(String.join(".", path), Optional.ofNullable(valueLeft).orElse(""), Optional.ofNullable(valueRight).orElse(""));
        }
    }
}
