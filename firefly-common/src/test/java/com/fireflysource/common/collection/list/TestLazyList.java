package com.fireflysource.common.collection.list;

import com.fireflysource.common.collection.array.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Tests for LazyList utility class.
 */
class TestLazyList {
    private static final boolean STRICT = false;

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_NullInput_NullItem() {
        Object list = LazyList.add(null, null);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_NullInput_NonListItem() {
        String item = "a";
        Object list = LazyList.add(null, item);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_NullInput_LazyListItem() {
        Object item = LazyList.add(null, "x");
        item = LazyList.add(item, "y");
        item = LazyList.add(item, "z");

        Object list = LazyList.add(null, item);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));

        Object val = LazyList.get(list, 0);
        assertTrue(val instanceof List);
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_NonListInput() {
        String input = "a";

        Object list = LazyList.add(input, "b");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(2, LazyList.size(list));
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_LazyListInput() {
        Object input = LazyList.add(null, "a");

        Object list = LazyList.add(input, "b");
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));

        list = LazyList.add(list, "c");
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");

        Object list = LazyList.add(input, "b");
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));

        list = LazyList.add(list, "c");
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#add(Object, Object)}
     */
    @Test
    void testAddObjectObject_AddNull() {
        Object list = null;
        list = LazyList.add(list, null);
        assertEquals(1, LazyList.size(list));
        assertNull(LazyList.get(list, 0));

        list = "a";
        list = LazyList.add(list, null);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertNull(LazyList.get(list, 1));

        list = LazyList.add(list, null);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertNull(LazyList.get(list, 1));
        assertNull(LazyList.get(list, 2));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NullInput_NullItem() {
        Object list = LazyList.add(null, 0, null);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NullInput_NonListItem() {
        String item = "a";
        Object list = LazyList.add(null, 0, item);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NullInput_NonListItem2() {
        assumeTrue(STRICT); // Only run in STRICT mode.

        String item = "a";
        // Test branch of logic "index>0"
        Object list = LazyList.add(null, 1, item); // Always throws exception?
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NullInput_LazyListItem() {
        Object item = LazyList.add(null, "x");
        item = LazyList.add(item, "y");
        item = LazyList.add(item, "z");

        Object list = LazyList.add(null, 0, item);
        assertNotNull(list);
        assertEquals(1, LazyList.size(list));

        Object val = LazyList.get(list, 0);
        assertTrue(val instanceof List);
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NullInput_GenericListItem() {
        List<String> item = new ArrayList<>();
        item.add("a");

        Object list = LazyList.add(null, 0, item);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NonListInput_NullItem() {
        String input = "a";

        Object list = LazyList.add(input, 0, null);
        assertNotNull(list);
        assertEquals(2, LazyList.size(list));
        assertNull(LazyList.get(list, 0));
        assertEquals("a", LazyList.get(list, 1));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_NonListInput_NonListItem() {
        String input = "a";
        String item = "b";

        Object list = LazyList.add(input, 0, item);
        assertNotNull(list);
        assertEquals(2, LazyList.size(list));
        assertEquals("b", LazyList.get(list, 0));
        assertEquals("a", LazyList.get(list, 1));
    }

    /**
     * Test for {@link LazyList#add(Object, int, Object)}
     */
    @Test
    void testAddObjectIntObject_LazyListInput() {
        Object list = LazyList.add(null, "c"); // [c]
        list = LazyList.add(list, 0, "a"); // [a, c]
        list = LazyList.add(list, 1, "b"); // [a, b, c]
        list = LazyList.add(list, 3, "d"); // [a, b, c, d]

        assertEquals(4, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
        assertEquals("d", LazyList.get(list, 3));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_NullInput() {
        Collection<?> coll = Arrays.asList("a", "b", "c");

        Object list = LazyList.addCollection(null, coll);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_NonListInput() {
        Collection<?> coll = Arrays.asList("a", "b", "c");
        String input = "z";

        Object list = LazyList.addCollection(input, coll);
        assertTrue(list instanceof List);
        assertEquals(4, LazyList.size(list));
        assertEquals("z", LazyList.get(list, 0));
        assertEquals("a", LazyList.get(list, 1));
        assertEquals("b", LazyList.get(list, 2));
        assertEquals("c", LazyList.get(list, 3));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_LazyListInput() {
        Collection<?> coll = Arrays.asList("a", "b", "c");

        Object input = LazyList.add(null, "x");
        input = LazyList.add(input, "y");
        input = LazyList.add(input, "z");

        Object list = LazyList.addCollection(input, coll);
        assertTrue(list instanceof List);
        assertEquals(6, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
        assertEquals("b", LazyList.get(list, 4));
        assertEquals("c", LazyList.get(list, 5));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_GenricListInput() {
        Collection<?> coll = Arrays.asList("a", "b", "c");

        List<String> input = new ArrayList<String>();
        input.add("x");
        input.add("y");
        input.add("z");

        Object list = LazyList.addCollection(input, coll);
        assertTrue(list instanceof List);
        assertEquals(6, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
        assertEquals("b", LazyList.get(list, 4));
        assertEquals("c", LazyList.get(list, 5));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_Sequential() {
        Collection<?> coll = Arrays.asList("a", "b");

        Object list = null;
        list = LazyList.addCollection(list, coll);
        list = LazyList.addCollection(list, coll);

        assertEquals(4, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("a", LazyList.get(list, 2));
        assertEquals("b", LazyList.get(list, 3));
    }

    /**
     * Test for {@link LazyList#addCollection(Object, Collection)}
     */
    @Test
    void testAddCollection_GenericListInput() {
        List<String> l = new ArrayList<>();
        l.add("a");
        l.add("b");

        Object list = null;
        list = LazyList.addCollection(list, l);
        list = LazyList.addCollection(list, l);

        assertEquals(4, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("a", LazyList.get(list, 2));
        assertEquals("b", LazyList.get(list, 3));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NullInput_NullArray() {
        String[] arr = null;
        Object list = LazyList.addArray(null, arr);
        assertNull(list);
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NullInput_EmptyArray() {
        String[] arr = new String[0];
        Object list = LazyList.addArray(null, arr);
        if (STRICT) {
            assertNotNull(list);
            assertTrue(list instanceof List);
        }
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NullInput_SingleArray() {
        String[] arr = new String[]{"a"};
        Object list = LazyList.addArray(null, arr);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NullInput_Array() {
        String[] arr = new String[]{"a", "b", "c"};
        Object list = LazyList.addArray(null, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NonListInput_NullArray() {
        String input = "z";
        String[] arr = null;
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));
        assertEquals("z", LazyList.get(list, 0));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NonListInput_EmptyArray() {
        String input = "z";
        String[] arr = new String[0];
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));
        assertEquals("z", LazyList.get(list, 0));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NonListInput_SingleArray() {
        String input = "z";
        String[] arr = new String[]{"a"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(2, LazyList.size(list));
        assertEquals("z", LazyList.get(list, 0));
        assertEquals("a", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_NonListInput_Array() {
        String input = "z";
        String[] arr = new String[]{"a", "b", "c"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(4, LazyList.size(list));
        assertEquals("z", LazyList.get(list, 0));
        assertEquals("a", LazyList.get(list, 1));
        assertEquals("b", LazyList.get(list, 2));
        assertEquals("c", LazyList.get(list, 3));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_LazyListInput_NullArray() {
        Object input = LazyList.add(null, "x");
        input = LazyList.add(input, "y");
        input = LazyList.add(input, "z");

        String[] arr = null;
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_LazyListInput_EmptyArray() {
        Object input = LazyList.add(null, "x");
        input = LazyList.add(input, "y");
        input = LazyList.add(input, "z");

        String[] arr = new String[0];
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_LazyListInput_SingleArray() {
        Object input = LazyList.add(null, "x");
        input = LazyList.add(input, "y");
        input = LazyList.add(input, "z");

        String[] arr = new String[]{"a"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(4, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_LazyListInput_Array() {
        Object input = LazyList.add(null, "x");
        input = LazyList.add(input, "y");
        input = LazyList.add(input, "z");

        String[] arr = new String[]{"a", "b", "c"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(6, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
        assertEquals("b", LazyList.get(list, 4));
        assertEquals("c", LazyList.get(list, 5));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_GenericListInput_NullArray() {
        List<String> input = new ArrayList<String>();
        input.add("x");
        input.add("y");
        input.add("z");

        String[] arr = null;
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_GenericListInput_EmptyArray() {
        List<String> input = new ArrayList<>();
        input.add("x");
        input.add("y");
        input.add("z");

        String[] arr = new String[0];
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_GenericListInput_SingleArray() {
        List<String> input = new ArrayList<String>();
        input.add("x");
        input.add("y");
        input.add("z");

        String[] arr = new String[]{"a"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(4, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
    }

    /**
     * Tests for {@link LazyList#addArray(Object, Object[])}
     */
    @Test
    void testAddArray_GenericListInput_Array() {
        List<String> input = new ArrayList<>();
        input.add("x");
        input.add("y");
        input.add("z");

        String[] arr = new String[]{"a", "b", "c"};
        Object list = LazyList.addArray(input, arr);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(6, LazyList.size(list));
        assertEquals("x", LazyList.get(list, 0));
        assertEquals("y", LazyList.get(list, 1));
        assertEquals("z", LazyList.get(list, 2));
        assertEquals("a", LazyList.get(list, 3));
        assertEquals("b", LazyList.get(list, 4));
        assertEquals("c", LazyList.get(list, 5));
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_NullInput() {
        Object list = LazyList.ensureSize(null, 10);
        assertNotNull(list);
        assertTrue(list instanceof List);
        // Not possible to test for List capacity value.
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_NonListInput() {
        String input = "a";
        Object list = LazyList.ensureSize(input, 10);
        assertNotNull(list);
        assertTrue(list instanceof List);
        // Not possible to test for List capacity value.
        assertEquals(1, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");

        Object list = LazyList.ensureSize(input, 10);
        assertNotNull(list);
        assertTrue(list instanceof List);
        // Not possible to test for List capacity value.
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");

        Object list = LazyList.ensureSize(input, 10);
        assertNotNull(list);
        assertTrue(list instanceof List);
        // Not possible to test for List capacity value.
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_GenericListInput_LinkedList() {
        assumeTrue(STRICT); // Only run in STRICT mode.

        // Using LinkedList concrete type as LazyList internal
        // implementation does not look for this specifically.
        List<String> input = new LinkedList<String>();
        input.add("a");
        input.add("b");

        Object list = LazyList.ensureSize(input, 10);
        assertNotNull(list);
        assertTrue(list instanceof List);
        // Not possible to test for List capacity value.
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_Growth() {
        List<String> l = new ArrayList<>();
        l.add("a");
        l.add("b");
        l.add("c");

        // NOTE: Testing for object equality might be viewed as
        // fragile by most developers, however, for this
        // specific implementation, we don't want the
        // provided list to change if the size requirements
        // have been met.

        // Trigger growth
        Object ret = LazyList.ensureSize(l, 10);
        assertNotSame(ret, l);

        // Growth not neeed.
        ret = LazyList.ensureSize(l, 1);
        assertSame(ret, l);
    }

    /**
     * Tests for {@link LazyList#ensureSize(Object, int)}
     */
    @Test
    void testEnsureSize_Growth_LinkedList() {
        assumeTrue(STRICT); // Only run in STRICT mode.

        // Using LinkedList concrete type as LazyList internal
        // implementation has not historically looked for this
        // specifically.
        List<String> l = new LinkedList<>();
        l.add("a");
        l.add("b");
        l.add("c");

        // NOTE: Testing for object equality might be viewed as
        // fragile by most developers, however, for this
        // specific implementation, we don't want the
        // provided list to change if the size requirements
        // have been met.

        // Trigger growth
        Object ret = LazyList.ensureSize(l, 10);
        assertTrue(ret != l);

        // Growth not neeed.
        ret = LazyList.ensureSize(l, 1);
        assertTrue(ret == l);
    }

    /**
     * Test for {@link LazyList#remove(Object, Object)}
     */
    @Test
    void testRemoveObjectObject_NullInput() {
        Object input = null;

        assertNull(LazyList.remove(input, null));
        assertNull(LazyList.remove(input, "a"));
        assertNull(LazyList.remove(input, new ArrayList<>()));
        assertNull(LazyList.remove(input, Integer.valueOf(42)));
    }

    /**
     * Test for {@link LazyList#remove(Object, Object)}
     */
    @Test
    void testRemoveObjectObject_NonListInput() {
        String input = "a";

        // Remove null item
        Object list = LazyList.remove(input, null);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));

        // Remove item that doesn't exist
        list = LazyList.remove(input, "b");
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));

        // Remove item that exists
        list = LazyList.remove(input, "a");
        // should this be null? or an empty list?
        assertNull(list); // nothing left in list
        assertEquals(0, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#remove(Object, Object)}
     */
    @Test
    void testRemoveObjectObject_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        // Remove null item
        Object list = LazyList.remove(input, null);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));

        // Attempt to remove something that doesn't exist
        list = LazyList.remove(input, "z");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));

        // Remove something that exists in input
        list = LazyList.remove(input, "b");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("c", LazyList.get(list, 1));
    }

    /**
     * Test for {@link LazyList#remove(Object, Object)}
     */
    @Test
    void testRemoveObjectObject_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("c");

        // Remove null item
        Object list = LazyList.remove(input, null);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(3, LazyList.size(list));

        // Attempt to remove something that doesn't exist
        list = LazyList.remove(input, "z");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(3, LazyList.size(list));

        // Remove something that exists in input
        list = LazyList.remove(input, "b");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("c", LazyList.get(list, 1));

        // Try to remove the rest.
        list = LazyList.remove(list, "a");
        list = LazyList.remove(list, "c");
        assertNull(list);
    }

    /**
     * Test for {@link LazyList#remove(Object, Object)}
     */
    @Test
    void testRemoveObjectObject_LinkedListInput() {
        // Should be able to use any collection object.
        List<String> input = new LinkedList<>();
        input.add("a");
        input.add("b");
        input.add("c");

        // Remove null item
        Object list = LazyList.remove(input, null);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(3, LazyList.size(list));

        // Attempt to remove something that doesn't exist
        list = LazyList.remove(input, "z");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(3, LazyList.size(list));

        // Remove something that exists in input
        list = LazyList.remove(input, "b");
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertTrue(input == list);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("c", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#remove(Object, int)}
     */
    @Test
    void testRemoveObjectInt_NullInput() {
        Object input = null;

        assertNull(LazyList.remove(input, 0));
        assertNull(LazyList.remove(input, 2));
        assertNull(LazyList.remove(input, -2));
    }

    /**
     * Tests for {@link LazyList#remove(Object, int)}
     */
    @Test
    void testRemoveObjectInt_NonListInput() {
        String input = "a";

        // Invalid index
        Object list = LazyList.remove(input, 1);
        assertNotNull(list);
        if (STRICT) {
            assertTrue(list instanceof List);
        }
        assertEquals(1, LazyList.size(list));

        // Valid index
        list = LazyList.remove(input, 0);
        // should this be null? or an empty list?
        assertNull(list); // nothing left in list
        assertEquals(0, LazyList.size(list));
    }

    /**
     * Tests for {@link LazyList#remove(Object, int)}
     */
    @Test
    void testRemoveObjectInt_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        Object list = null;

        if (STRICT) {
            // Invalid index
            // Shouldn't cause a IndexOutOfBoundsException as this is not the
            // same behavior you experience in testRemoveObjectInt_NonListInput
            // and
            // testRemoveObjectInt_NullInput when using invalid indexes.
            list = LazyList.remove(input, 5);
            assertNotNull(list);
            assertTrue(list instanceof List);
            assertEquals(3, LazyList.size(list));
        }

        // Valid index
        list = LazyList.remove(input, 1); // remove the 'b'
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("c", LazyList.get(list, 1));
    }

    /**
     * Tests for {@link LazyList#remove(Object, int)}
     */
    @Test
    void testRemoveObjectInt_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        Object list = null;

        if (STRICT) {
            // Invalid index
            // Shouldn't cause a IndexOutOfBoundsException as this is not the
            // same behavior you experience in testRemoveObjectInt_NonListInput
            // and
            // testRemoveObjectInt_NullInput when using invalid indexes.
            list = LazyList.remove(input, 5);
            assertNotNull(list);
            assertTrue(list instanceof List);
            assertEquals(3, LazyList.size(list));
        }

        // Valid index
        list = LazyList.remove(input, 1); // remove the 'b'
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(2, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("c", LazyList.get(list, 1));

        // Remove the rest
        list = LazyList.remove(list, 0); // the 'a'
        list = LazyList.remove(list, 0); // the 'c'
        assertNull(list);
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_NullInput() {
        Object input = null;

        Object list = LazyList.getList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(0, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_NonListInput() {
        String input = "a";

        Object list = LazyList.getList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        Object list = LazyList.getList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        Object list = LazyList.getList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_LinkedListInput() {
        List<String> input = new LinkedList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        Object list = LazyList.getList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Test for {@link LazyList#getList(Object)}
     */
    @Test
    void testGetListObject_NullForEmpty() {
        assertNull(LazyList.getList(null, true));
        assertNotNull(LazyList.getList(null, false));
    }

    /**
     * Tests for {@link LazyList#toStringArray(Object)}
     */
    @Test
    @SuppressWarnings("unchecked")
    void testToStringArray() {
        assertEquals(0, LazyList.toStringArray(null).length);

        assertEquals(1, LazyList.toStringArray("a").length);
        assertEquals("a", LazyList.toStringArray("a")[0]);

        @SuppressWarnings("rawtypes")
        ArrayList l = new ArrayList();
        l.add("a");
        l.add(null);
        l.add(new Integer(2));
        String[] a = LazyList.toStringArray(l);

        assertEquals(3, a.length);
        assertEquals("a", a[0]);
        assertEquals(null, a[1]);
        assertEquals("2", a[2]);

    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_NullInput_Object() {
        Object input = null;

        Object arr = LazyList.toArray(input, Object.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_NullInput_String() {
        String input = null;

        Object arr = LazyList.toArray(input, String.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
        assertTrue(arr instanceof String[]);
    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_NonListInput() {
        String input = "a";

        Object arr = LazyList.toArray(input, String.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
        assertTrue(arr instanceof String[]);

        String[] strs = (String[]) arr;
        assertEquals(1, strs.length);
        assertEquals("a", strs[0]);
    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        Object arr = LazyList.toArray(input, String.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
        assertTrue(arr instanceof String[]);

        String[] strs = (String[]) arr;
        assertEquals(3, strs.length);
        assertEquals("a", strs[0]);
        assertEquals("b", strs[1]);
        assertEquals("c", strs[2]);
    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_LazyListInput_Primitives() {
        Object input = LazyList.add(null, 22);
        input = LazyList.add(input, 333);
        input = LazyList.add(input, 4444);
        input = LazyList.add(input, 55555);

        Object arr = LazyList.toArray(input, int.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
        assertTrue(arr instanceof int[]);

        int[] nums = (int[]) arr;
        assertEquals(4, nums.length);
        assertEquals(22, nums[0]);
        assertEquals(333, nums[1]);
        assertEquals(4444, nums[2]);
        assertEquals(55555, nums[3]);
    }

    /**
     * Tests for {@link LazyList#toArray(Object, Class)}
     */
    @Test
    void testToArray_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        Object arr = LazyList.toArray(input, String.class);
        assertNotNull(arr);
        assertTrue(arr.getClass().isArray());
        assertTrue(arr instanceof String[]);

        String[] strs = (String[]) arr;
        assertEquals(3, strs.length);
        assertEquals("a", strs[0]);
        assertEquals("b", strs[1]);
        assertEquals("c", strs[2]);
    }

    /**
     * Tests for {@link LazyList#size(Object)}
     */
    @Test
    void testSize_NullInput() {
        assertEquals(0, LazyList.size(null));
    }

    /**
     * Tests for {@link LazyList#size(Object)}
     */
    @Test
    void testSize_NonListInput() {
        String input = "a";
        assertEquals(1, LazyList.size(input));
    }

    /**
     * Tests for {@link LazyList#size(Object)}
     */
    @Test
    void testSize_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");

        assertEquals(2, LazyList.size(input));

        input = LazyList.add(input, "c");

        assertEquals(3, LazyList.size(input));
    }

    /**
     * Tests for {@link LazyList#size(Object)}
     */
    @Test
    void testSize_GenericListInput() {
        List<String> input = new ArrayList<String>();

        assertEquals(0, LazyList.size(input));

        input.add("a");
        input.add("b");

        assertEquals(2, LazyList.size(input));

        input.add("c");

        assertEquals(3, LazyList.size(input));
    }

    /**
     * Tests for bad input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_OutOfBounds_NullInput() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            LazyList.get(null, 0); // Should Fail due to null input
        });
    }

    /**
     * Tests for bad input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_OutOfBounds_NonListInput() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            String input = "a";
            LazyList.get(input, 1); // Should Fail
        });
    }

    /**
     * Tests for bad input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_OutOfBounds_LazyListInput() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Object input = LazyList.add(null, "a");
            LazyList.get(input, 1); // Should Fail
        });
    }

    /**
     * Tests for bad input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_OutOfBounds_GenericListInput() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            List<String> input = new ArrayList<>();
            input.add("a");
            LazyList.get(input, 1); // Should Fail
        });
    }

    /**
     * Tests for non-list input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_NonListInput() {
        String input = "a";
        assertEquals("a", LazyList.get(input, 0));
    }

    /**
     * Tests for list input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_LazyListInput() {
        Object input = LazyList.add(null, "a");
        assertEquals("a", LazyList.get(input, 0));
    }

    /**
     * Tests for list input on {@link LazyList#get(Object, int)}
     */
    @Test
    void testGet_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");
        assertEquals("a", LazyList.get(input, 0));

        List<URI> uris = new ArrayList<>();
        uris.add(URI.create("http://www.abc.org/"));
        uris.add(URI.create("http://www.cde.ort/firefly/"));
        uris.add(URI.create("http://www.fgh.com/firefly/"));
        uris.add(URI.create("http://www.fireflysource.com/"));

        // Make sure that Generics pass through the 'get' routine safely.
        // We should be able to call this without casting the result to URI
        URI eclipseUri = LazyList.get(uris, 3);
        assertEquals("http://www.fireflysource.com/", eclipseUri.toASCIIString());
    }

    /**
     * Tests for {@link LazyList#contains(Object, Object)}
     */
    @Test
    void testContains_NullInput() {
        assertFalse(LazyList.contains(null, "z"));
    }

    /**
     * Tests for {@link LazyList#contains(Object, Object)}
     */
    @Test
    void testContains_NonListInput() {
        String input = "a";
        assertFalse(LazyList.contains(input, "z"));
        assertTrue(LazyList.contains(input, "a"));
    }

    /**
     * Tests for {@link LazyList#contains(Object, Object)}
     */
    @Test
    void testContains_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        assertFalse(LazyList.contains(input, "z"));
        assertTrue(LazyList.contains(input, "a"));
        assertTrue(LazyList.contains(input, "b"));
    }

    /**
     * Tests for {@link LazyList#contains(Object, Object)}
     */
    @Test
    void testContains_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("c");

        assertFalse(LazyList.contains(input, "z"));
        assertTrue(LazyList.contains(input, "a"));
        assertTrue(LazyList.contains(input, "b"));
    }

    /**
     * Tests for {@link LazyList#clone(Object)}
     */
    @Test
    void testClone_NullInput() {
        Object input = null;

        Object list = LazyList.clone(input);
        assertNull(list);
    }

    /**
     * Tests for {@link LazyList#clone(Object)}
     */
    @Test
    void testClone_NonListInput() {
        String input = "a";

        Object list = LazyList.clone(input);
        assertNotNull(list);
        assertSame(input, list);
    }

    /**
     * Tests for {@link LazyList#clone(Object)}
     */
    @Test
    void testClone_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        Object list = LazyList.clone(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertNotSame(input, list);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#clone(Object)}
     */
    @Test
    void testClone_GenericListInput() {
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("c");

        // decorate the .clone(Object) method to return
        // the same generic object element type
        Object list = LazyList.clone(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertNotSame(input, list);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link LazyList#toString(Object)}
     */
    @Test
    void testToString_NullInput() {
        Object input = null;
        assertEquals("[]", LazyList.toString(input));
    }

    /**
     * Tests for {@link LazyList#toString(Object)}
     */
    @Test
    void testToString_NonListInput() {
        String input = "a";
        assertEquals("[a]", LazyList.toString(input));
    }

    /**
     * Tests for {@link LazyList#toString(Object)}
     */
    @Test
    void testToString_LazyListInput() {
        Object input = LazyList.add(null, "a");

        assertEquals("[a]", LazyList.toString(input));

        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        assertEquals("[a, b, c]", LazyList.toString(input));
    }

    /**
     * Tests for {@link LazyList#toString(Object)}
     */
    @Test
    void testToString_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");

        assertEquals("[a]", LazyList.toString(input));

        input.add("b");
        input.add("c");

        assertEquals("[a, b, c]", LazyList.toString(input));
    }

    /**
     * Tests for {@link LazyList#iterator(Object)}
     */
    @Test
    void testIterator_NullInput() {
        Iterator<?> iter = LazyList.iterator(null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    /**
     * Tests for {@link LazyList#iterator(Object)}
     */
    @Test
    void testIterator_NonListInput() {
        String input = "a";

        Iterator<?> iter = LazyList.iterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    /**
     * Tests for {@link LazyList#iterator(Object)}
     */
    @Test
    void testIterator_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        Iterator<?> iter = LazyList.iterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    /**
     * Tests for {@link LazyList#iterator(Object)}
     */
    @Test
    void testIterator_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        Iterator<String> iter = LazyList.iterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    /**
     * Tests for {@link LazyList#listIterator(Object)}
     */
    @Test
    void testListIterator_NullInput() {
        ListIterator<?> iter = LazyList.listIterator(null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertFalse(iter.hasPrevious());
    }

    /**
     * Tests for {@link LazyList#listIterator(Object)}
     */
    @Test
    void testListIterator_NonListInput() {
        String input = "a";

        ListIterator<?> iter = LazyList.listIterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
        assertTrue(iter.hasPrevious());
    }

    /**
     * Tests for {@link LazyList#listIterator(Object)}
     */
    @Test
    void testListIterator_LazyListInput() {
        Object input = LazyList.add(null, "a");
        input = LazyList.add(input, "b");
        input = LazyList.add(input, "c");

        ListIterator<?> iter = LazyList.listIterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals("c", iter.previous());
        assertEquals("b", iter.previous());
        assertEquals("a", iter.previous());
        assertFalse(iter.hasPrevious());
    }

    /**
     * Tests for {@link LazyList#listIterator(Object)}
     */
    @Test
    void testListIterator_GenericListInput() {
        List<String> input = new ArrayList<String>();
        input.add("a");
        input.add("b");
        input.add("c");

        ListIterator<?> iter = LazyList.listIterator(input);
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals("c", iter.previous());
        assertEquals("b", iter.previous());
        assertEquals("a", iter.previous());
        assertFalse(iter.hasPrevious());
    }

    /**
     * Tests for {@link ArrayUtils#asMutableList(Object[])}
     */
    @Test
    void testArray2List_NullInput() {
        Object[] input = null;
        Object list = ArrayUtils.asMutableList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(0, LazyList.size(list));
    }

    /**
     * Tests for {@link ArrayUtils#asMutableList(Object[])}
     */
    @Test
    void testArray2List_EmptyInput() {
        String[] input = new String[0];

        Object list = ArrayUtils.asMutableList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(0, LazyList.size(list));
    }

    /**
     * Tests for {@link ArrayUtils#asMutableList(Object[])}
     */
    @Test
    void testArray2List_SingleInput() {
        String[] input = new String[]{"a"};

        Object list = ArrayUtils.asMutableList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(1, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
    }

    /**
     * Tests for {@link ArrayUtils#asMutableList(Object[])}
     */
    @Test
    void testArray2List_MultiInput() {
        String[] input = new String[]{"a", "b", "c"};

        Object list = ArrayUtils.asMutableList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link ArrayUtils#asMutableList(Object[])}
     */
    @Test
    void testArray2List_GenericsInput() {
        String[] input = new String[]{"a", "b", "c"};

        // Test the Generics definitions for array2List
        List<String> list = ArrayUtils.asMutableList(input);
        assertNotNull(list);
        assertTrue(list instanceof List);
        assertEquals(3, LazyList.size(list));
        assertEquals("a", LazyList.get(list, 0));
        assertEquals("b", LazyList.get(list, 1));
        assertEquals("c", LazyList.get(list, 2));
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_NullInput_NullItem() {
        Object[] input = null;

        Object[] arr = ArrayUtils.addToArray(input, null, Object.class);
        assertNotNull(arr);
        if (STRICT) {
            // Adding null item to array should result in nothing added?
            assertEquals(0, arr.length);
        } else {
            assertEquals(1, arr.length);
        }
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_NullNullNull() {
        // NPE if item && type are both null.
        assumeTrue(STRICT);

        // Harsh test case.
        Object[] input = null;

        Object[] arr = ArrayUtils.addToArray(input, null, null);
        assertNotNull(arr);
        if (STRICT) {
            // Adding null item to array should result in nothing added?
            assertEquals(0, arr.length);
        } else {
            assertEquals(1, arr.length);
        }
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_NullInput_SimpleItem() {
        Object[] input = null;

        Object[] arr = ArrayUtils.addToArray(input, "a", String.class);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals("a", arr[0]);

        // Same test, but with an undefined type
        arr = ArrayUtils.addToArray(input, "b", null);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals("b", arr[0]);
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_EmptyInput_NullItem() {
        String[] input = new String[0];

        String[] arr = ArrayUtils.addToArray(input, null, Object.class);
        assertNotNull(arr);
        if (STRICT) {
            // Adding null item to array should result in nothing added?
            assertEquals(0, arr.length);
        } else {
            assertEquals(1, arr.length);
        }
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_EmptyInput_SimpleItem() {
        String[] input = new String[0];

        String[] arr = ArrayUtils.addToArray(input, "a", String.class);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals("a", arr[0]);
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_SingleInput_NullItem() {
        String[] input = new String[]{"z"};

        String[] arr = ArrayUtils.addToArray(input, null, Object.class);
        assertNotNull(arr);
        if (STRICT) {
            // Should a null item be added to an array?
            assertEquals(1, arr.length);
        } else {
            assertEquals(2, arr.length);
            assertEquals("z", arr[0]);
            assertEquals(null, arr[1]);
        }
    }

    /**
     * Tests for {@link ArrayUtils#addToArray(Object[], Object, Class)}
     */
    @Test
    void testAddToArray_SingleInput_SimpleItem() {
        String[] input = new String[]{"z"};

        String[] arr = ArrayUtils.addToArray(input, "a", String.class);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertEquals("z", arr[0]);
        assertEquals("a", arr[1]);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_NullInput_NullItem() {
        Object[] input = null;

        Object[] arr = ArrayUtils.removeFromArray(input, null);
        assertNull(arr);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_NullInput_SimpleItem() {
        Object[] input = null;

        Object[] arr = ArrayUtils.removeFromArray(input, "a");
        assertNull(arr);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_EmptyInput_NullItem() {
        String[] input = new String[0];

        String[] arr = ArrayUtils.removeFromArray(input, null);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_EmptyInput_SimpleItem() {
        String[] input = new String[0];

        String[] arr = ArrayUtils.removeFromArray(input, "a");
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_SingleInput() {
        String[] input = new String[]{"a"};

        String[] arr = ArrayUtils.removeFromArray(input, null);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals("a", arr[0]);

        // Remove actual item
        arr = ArrayUtils.removeFromArray(input, "a");
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    /**
     * Tests for {@link ArrayUtils#removeFromArray(Object[], Object)}
     */
    @Test
    void testRemoveFromArray_MultiInput() {
        String[] input = new String[]{"a", "b", "c"};

        String[] arr = ArrayUtils.removeFromArray(input, null);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);

        // Remove an actual item
        arr = ArrayUtils.removeFromArray(input, "b");
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("c", arr[1]);
    }
}
