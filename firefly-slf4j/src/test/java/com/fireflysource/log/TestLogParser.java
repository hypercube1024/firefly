package com.fireflysource.log;


import com.fireflysource.log.internal.utils.collection.TreeTrie;
import com.fireflysource.log.internal.utils.collection.Trie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestLogParser {

    @Test
    void test() {
        Trie<Log> xmlLogTree = new TreeTrie<>();
        LogConfigParser parser = new XmlLogConfigParser();
        boolean success = parser.parse((fileLog) -> xmlLogTree.put(fileLog.getName(), fileLog));
        assertTrue(success);

        Log bar = xmlLogTree.getBest("com.fireflysource.log.foo.bar.Bar");
        assertEquals("com.fireflysource.log.foo.bar", bar.getName());

        Log debug = xmlLogTree.getBest("test-DEBUG");
        assertFalse(debug.isTraceEnabled());
        assertTrue(debug.isDebugEnabled());
        assertTrue(debug.isInfoEnabled());

        System.out.println(debug.getClass().getName());
    }
}
