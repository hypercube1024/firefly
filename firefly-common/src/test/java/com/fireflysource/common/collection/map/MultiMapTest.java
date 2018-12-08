package com.fireflysource.common.collection.map;


import com.fireflysource.common.collection.list.LazyList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiMapTest {
    /**
     * Tests {@link MultiMap#put(Object, Object)}
     */
    @Test
    void testPut() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");
    }

    /**
     * Tests {@link MultiMap#put(Object, Object)}
     */
    @Test
    void testPut_Null_String() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";
        String val = null;

        mm.put(key, val);
        assertMapSize(mm, 1);
        assertNullValues(mm, key);
    }

    /**
     * Tests {@link MultiMap#put(Object, Object)}
     */
    @Test
    void testPut_Null_List() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";
        List<String> vals = null;

        mm.put(key, vals);
        assertMapSize(mm, 1);
        assertNullValues(mm, key);
    }

    /**
     * Tests {@link MultiMap#put(Object, Object)}
     */
    @Test
    void testPut_Replace() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";
        Object ret;

        ret = mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");
        assertNull(ret);
        Object orig = mm.get(key);

        // Now replace it
        ret = mm.put(key, "jar");
        assertMapSize(mm, 1);
        assertValues(mm, key, "jar");
        assertEquals(orig, ret);
    }

    /**
     * Tests {@link MultiMap#putValues(String, List)}
     */
    @Test
    public void testPutValues_List() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        List<String> input = new ArrayList<String>();
        input.add("gzip");
        input.add("jar");
        input.add("pack200");

        mm.putValues(key, input);
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");
    }

    @Test
    public void testPutValues_StringArray() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        String input[] = {"gzip", "jar", "pack200"};
        mm.putValues(key, input);
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");
    }

    @Test
    public void testPutValues_VarArgs() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        mm.putValues(key, "gzip", "jar", "pack200");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");
    }

    /**
     * Tests {@link MultiMap#add(String, Object)}
     */
    @Test
    public void testAdd() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");

        // Add to the key
        mm.add(key, "jar");
        mm.add(key, "pack200");

        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");
    }

    /**
     * Tests {@link MultiMap#addValues(String, List)}
     */
    @Test
    public void testAddValues_List() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");

        // Add to the key
        List<String> extras = new ArrayList<String>();
        extras.add("jar");
        extras.add("pack200");
        extras.add("zip");
        mm.addValues(key, extras);

        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200", "zip");
    }

    /**
     * Tests {@link MultiMap#addValues(String, List)}
     */
    @Test
    public void testAddValues_List_Empty() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");

        // Add to the key
        List<String> extras = new ArrayList<String>();
        mm.addValues(key, extras);

        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");
    }

    /**
     * Tests {@link MultiMap#addValues(String, Object[])}
     */
    @Test
    public void testAddValues_StringArray() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");

        // Add to the key
        String extras[] = {"jar", "pack200", "zip"};
        mm.addValues(key, extras);

        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200", "zip");
    }

    /**
     * Tests {@link MultiMap#addValues(String, Object[])}
     */
    @Test
    public void testAddValues_StringArray_Empty() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.put(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");

        // Add to the key
        String extras[] = new String[0];
        mm.addValues(key, extras);

        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip");
    }

    /**
     * Tests {@link MultiMap#removeValue(String, Object)}
     */
    @Test
    public void testRemoveValue() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.putValues(key, "gzip", "jar", "pack200");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");

        // Remove a value
        mm.removeValue(key, "jar");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "pack200");

    }

    /**
     * Tests {@link MultiMap#removeValue(String, Object)}
     */
    @Test
    public void testRemoveValue_InvalidItem() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.putValues(key, "gzip", "jar", "pack200");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");

        // Remove a value that isn't there
        mm.removeValue(key, "msi");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");
    }

    /**
     * Tests {@link MultiMap#removeValue(String, Object)}
     */
    @Test
    public void testRemoveValue_AllItems() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.putValues(key, "gzip", "jar", "pack200");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "jar", "pack200");

        // Remove a value
        mm.removeValue(key, "jar");
        assertMapSize(mm, 1);
        assertValues(mm, key, "gzip", "pack200");

        // Remove another value
        mm.removeValue(key, "gzip");
        assertMapSize(mm, 1);
        assertValues(mm, key, "pack200");

        // Remove last value
        mm.removeValue(key, "pack200");
        assertMapSize(mm, 0); // should be empty now
    }

    /**
     * Tests {@link MultiMap#removeValue(String, Object)}
     */
    @Test
    public void testRemoveValue_FromEmpty() {
        MultiMap<String> mm = new MultiMap<>();

        String key = "formats";

        // Setup the key
        mm.putValues(key, new String[0]);
        assertMapSize(mm, 1);
        assertEmptyValues(mm, key);

        // Remove a value that isn't in the underlying values
        mm.removeValue(key, "jar");
        assertMapSize(mm, 1);
        assertEmptyValues(mm, key);
    }

    /**
     * Tests {@link MultiMap#putAll(Map)}
     */
    @Test
    public void testPutAll_Map() {
        MultiMap<String> mm = new MultiMap<>();

        assertMapSize(mm, 0); // Shouldn't have anything yet.

        Map<String, String> input = new HashMap<String, String>();
        input.put("food", "apple");
        input.put("color", "red");
        input.put("amount", "bushel");

        mm.putAllValues(input);

        assertMapSize(mm, 3);
        assertValues(mm, "food", "apple");
        assertValues(mm, "color", "red");
        assertValues(mm, "amount", "bushel");
    }

    /**
     * Tests {@link MultiMap#putAll(Map)}
     */
    @Test
    public void testPutAll_MultiMap_Simple() {
        MultiMap<String> mm = new MultiMap<>();

        assertMapSize(mm, 0); // Shouldn't have anything yet.

        MultiMap<String> input = new MultiMap<>();
        input.put("food", "apple");
        input.put("color", "red");
        input.put("amount", "bushel");

        mm.putAll(input);

        assertMapSize(mm, 3);
        assertValues(mm, "food", "apple");
        assertValues(mm, "color", "red");
        assertValues(mm, "amount", "bushel");
    }

    /**
     * Tests {@link MultiMap#putAll(Map)}
     */
    @Test
    public void testPutAll_MultiMapComplex() {
        MultiMap<String> mm = new MultiMap<>();

        assertMapSize(mm, 0); // Shouldn't have anything yet.

        MultiMap<String> input = new MultiMap<>();
        input.putValues("food", "apple", "cherry", "raspberry");
        input.put("color", "red");
        input.putValues("amount", "bushel", "pint");

        mm.putAll(input);

        assertMapSize(mm, 3);
        assertValues(mm, "food", "apple", "cherry", "raspberry");
        assertValues(mm, "color", "red");
        assertValues(mm, "amount", "bushel", "pint");
    }

    /**
     * Tests {@link MultiMap#toStringArrayMap()}
     */
    @Test
    public void testToStringArrayMap() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        assertMapSize(mm, 3);

        Map<String, String[]> sam = mm.toStringArrayMap();
        assertEquals(3, sam.size());

        assertArray("toStringArrayMap(food)", sam.get("food"), "apple", "cherry", "raspberry");
        assertArray("toStringArrayMap(color)", sam.get("color"), "red");
        assertArray("toStringArrayMap(amount)", sam.get("amount"), "bushel", "pint");
    }

    /**
     * Tests {@link MultiMap#toString()}
     */
    @Test
    public void testToString() {
        MultiMap<String> mm = new MultiMap<>();
        mm.put("color", "red");
        assertEquals("{color=red}", mm.toString());

        mm.putValues("food", "apple", "cherry", "raspberry");
        assertEquals("{color=red, food=[apple, cherry, raspberry]}", mm.toString());
    }

    /**
     * Tests {@link MultiMap#clear()}
     */
    @Test
    public void testClear() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        assertMapSize(mm, 3);

        mm.clear();

        assertMapSize(mm, 0);
    }

    /**
     * Tests {@link MultiMap#containsKey(Object)}
     */
    @Test
    public void testContainsKey() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        assertTrue(mm.containsKey("color"));
        assertFalse(mm.containsKey("nutrition"));
    }

    /**
     * Tests {@link MultiMap#containsSimpleValue(Object)}
     */
    @Test
    public void testContainsSimpleValue() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        assertTrue(mm.containsSimpleValue("red"));
        assertFalse(mm.containsValue("nutrition"));
    }

    /**
     * Tests {@link MultiMap#containsValue(Object)}
     */
    @Test
    public void testContainsValue() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        List<String> acr = new ArrayList<>();
        acr.add("apple");
        acr.add("cherry");
        acr.add("raspberry");
        assertTrue(mm.containsValue(acr));
        assertFalse(mm.containsValue("nutrition"));
    }

    /**
     * Tests {@link MultiMap#containsValue(Object)}
     */
    @Test
    public void testContainsValue_LazyList() {
        MultiMap<String> mm = new MultiMap<>();
        mm.putValues("food", "apple", "cherry", "raspberry");
        mm.put("color", "red");
        mm.putValues("amount", "bushel", "pint");

        Object list = LazyList.add(null, "bushel");
        list = LazyList.add(list, "pint");

        assertTrue(mm.containsValue(list));
    }

    private void assertArray(String prefix, Object[] actualValues, Object... expectedValues) {
        assertEquals(expectedValues.length, actualValues.length);
        int len = actualValues.length;
        for (int i = 0; i < len; i++) {
            assertEquals(expectedValues[i], actualValues[i]);
        }
    }

    private void assertValues(MultiMap<String> mm, String key, Object... expectedValues) {
        List<String> values = mm.getValues(key);
        assertEquals(expectedValues.length, values.size());
        int len = expectedValues.length;
        for (int i = 0; i < len; i++) {
            if (expectedValues[i] == null) {
                assertNull(values.get(i));
            } else {
                assertEquals(expectedValues[i], values.get(i));
            }
        }
    }

    private void assertNullValues(MultiMap<String> mm, String key) {
        List<String> values = mm.getValues(key);
        assertNull(values);
    }

    private void assertEmptyValues(MultiMap<String> mm, String key) {
        List<String> values = mm.getValues(key);
        assertEquals(0, LazyList.size(values));
    }

    private void assertMapSize(MultiMap<String> mm, int expectedSize) {
        assertEquals(expectedSize, mm.size());
    }
}
