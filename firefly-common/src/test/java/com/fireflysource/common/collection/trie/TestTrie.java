package com.fireflysource.common.collection.trie;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTrie {

    @Test
    void test() {
        Trie<String> trie = new TreeTrie<>();
        trie.put("com.firefly.foo.bar");
        trie.put("com.firefly.foo");

        assertEquals(2, trie.keySet().size());
        assertEquals("com.firefly.foo", trie.getBest("com.firefly.foo.Test"));
        assertEquals("com.firefly.foo.bar", trie.getBest("com.firefly.foo.bar.Hello"));
    }

}
