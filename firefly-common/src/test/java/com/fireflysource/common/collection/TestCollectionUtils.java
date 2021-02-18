package com.fireflysource.common.collection;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static com.fireflysource.common.collection.CollectionUtils.newHashSet;
import static org.junit.jupiter.api.Assertions.*;

class TestCollectionUtils {

    @Test
    void testIsEmpty() {
        assertTrue(CollectionUtils.isEmpty(Collections.emptyMap()));
        assertTrue(CollectionUtils.isEmpty(Collections.emptyList()));
        assertTrue(CollectionUtils.isEmpty(new HashMap<>()));
        Set<Object> map = null;
        assertTrue(CollectionUtils.isEmpty(map));
    }

    @Test
    void testIntersection() {
        Set<Integer> a = newHashSet(2, 3, 4);
        Set<Integer> b = newHashSet(3, 4, 5, 6);
        Set<Integer> result = CollectionUtils.intersect(a, b);

        assertEquals(3, a.size());
        assertEquals(4, b.size());
        assertEquals(newHashSet(3, 4), result);
    }

    @Test
    void testIntersectionEmpty() {
        Set<Integer> a = newHashSet(2, 3, 4);
        Set<Integer> b = newHashSet(5, 6);
        Set<Integer> result = CollectionUtils.intersect(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    void testHasIntersection() {
        Set<Integer> a = newHashSet(2, 3, 4);
        Set<Integer> b = newHashSet(3, 4, 5, 6);
        assertTrue(CollectionUtils.hasIntersection(a, b));
    }

    @Test
    void testNoIntersection() {
        Set<Integer> a = newHashSet(1, 2, 3, 4);
        Set<Integer> b = newHashSet(5, 6);
        assertFalse(CollectionUtils.hasIntersection(a, b));
        assertFalse(CollectionUtils.hasIntersection(a, newHashSet()));
        assertFalse(CollectionUtils.hasIntersection(newHashSet(), b));
        assertFalse(CollectionUtils.hasIntersection(newHashSet(), newHashSet()));
    }

    @Test
    void testUnion() {
        Set<Integer> a = newHashSet(2, 3, 4);
        Set<Integer> b = newHashSet(3, 4, 5, 6);
        Set<Integer> result = CollectionUtils.union(a, b);
        assertEquals(newHashSet(2, 3, 4, 5, 6), result);

        result = CollectionUtils.union(a, newHashSet());
        assertEquals(a, result);
    }

}
