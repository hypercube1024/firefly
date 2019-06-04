package com.fireflysource.common.collection;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionUtils {

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static <T> Set<T> intersect(Set<T> a, Set<T> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return new HashSet<>();
        } else {
            Set<T> set = new HashSet<>(a);
            set.retainAll(b);
            return set;
        }
    }

    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> set = new HashSet<>(a);
        set.addAll(b);
        return set;
    }

    public static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }

        if (a.size() < b.size()) {
            if (a.size() < 8) {
                return a.stream().anyMatch(b::contains);
            } else {
                return a.parallelStream().anyMatch(b::contains);
            }
        } else {
            if (b.size() < 8) {
                return b.stream().anyMatch(a::contains);
            } else {
                return b.parallelStream().anyMatch(a::contains);
            }
        }
    }

    @SafeVarargs
    public static <T> Set<T> newHashSet(T... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }

}
