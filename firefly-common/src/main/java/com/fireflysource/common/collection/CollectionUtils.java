package com.fireflysource.common.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static <T> Set<T> intersect(Set<T> a, Set<T> b) {
        Set<T> set = new HashSet<>(a);
        set.retainAll(b);
        return set;
    }

    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> set = new HashSet<>(a);
        set.addAll(b);
        return set;
    }

    public static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        return a.parallelStream().anyMatch(b::contains);
    }

    public static Map<String, Set<String>> merge(Stream<Map<String, Set<String>>> c, BinaryOperator<Set<String>> mergeFunction) {
        return c.flatMap(cvMapInVt -> cvMapInVt.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction));
    }
}
