package com.fireflysource.common.collection.trie;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTrie {

    @ParameterizedTest
    @ValueSource(strings = {"TreeTrie", "ArrayTernaryTrie", "ArrayTrie"})
    void test(String type) {
        Trie<String> trie;
        switch (type) {
            case "TreeTrie":
                trie = new TreeTrie<>();
                break;
            case "ArrayTernaryTrie":
                trie = new ArrayTernaryTrie<>(500);
                break;
            case "ArrayTrie":
                trie = new ArrayTrie<>(500);
                break;
            default:
                trie = new TreeTrie<>();
        }

        trie.put("com.firefly.foo.bar");
        trie.put("com.firefly.foo");

        assertEquals(2, trie.keySet().size());
        assertEquals("com.firefly.foo", trie.getBest("com.firefly.foo.Test"));
        assertEquals("com.firefly.foo.bar", trie.getBest("com.firefly.foo.bar.Hello"));
    }

}
