package com.fireflysource.net.websocket.utils;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test QuoteUtil
 */
public class QuoteUtilTest {
    private void assertSplitAt(Iterator<String> iter, String... expectedParts) {
        int len = expectedParts.length;
        for (int i = 0; i < len; i++) {
            String expected = expectedParts[i];
            assertTrue(iter.hasNext());
            assertEquals(expected, iter.next());
        }
    }

    @Test
    public void testSplitAtPreserveQuoting() {
        Iterator<String> iter = QuoteUtil.splitAt("permessage-compress; method=\"foo, bar\"", ";");
        assertSplitAt(iter, "permessage-compress", "method=\"foo, bar\"");
    }

    @Test
    public void testSplitAtPreserveQuotingWithNestedDelim() {
        Iterator<String> iter = QuoteUtil.splitAt("permessage-compress; method=\"foo; x=10\"", ";");
        assertSplitAt(iter, "permessage-compress", "method=\"foo; x=10\"");
    }

    @Test
    public void testSplitAtAllWhitespace() {
        Iterator<String> iter = QuoteUtil.splitAt("   ", "=");
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testSplitAtEmpty() {
        Iterator<String> iter = QuoteUtil.splitAt("", "=");
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testSplitAtHelloWorld() {
        Iterator<String> iter = QuoteUtil.splitAt("Hello World", " =");
        assertSplitAt(iter, "Hello", "World");
    }

    @Test
    public void testSplitAtKeyValueMessage() {
        Iterator<String> iter = QuoteUtil.splitAt("method=\"foo, bar\"", "=");
        assertSplitAt(iter, "method", "foo, bar");
    }

    @Test
    public void testSplitAtQuotedDelim() {
        // test that split ignores delimiters that occur within a quoted
        // part of the sequence.
        Iterator<String> iter = QuoteUtil.splitAt("A,\"B,C\",D", ",");
        assertSplitAt(iter, "A", "B,C", "D");
    }

    @Test
    public void testSplitAtSimple() {
        Iterator<String> iter = QuoteUtil.splitAt("Hi", "=");
        assertSplitAt(iter, "Hi");
    }

    @Test
    public void testSplitKeyValueQuoted() {
        Iterator<String> iter = QuoteUtil.splitAt("Key = \"Value\"", "=");
        assertSplitAt(iter, "Key", "Value");
    }

    @Test
    public void testSplitKeyValueQuotedValueList() {
        Iterator<String> iter = QuoteUtil.splitAt("Fruit = \"Apple, Banana, Cherry\"", "=");
        assertSplitAt(iter, "Fruit", "Apple, Banana, Cherry");
    }

    @Test
    public void testSplitKeyValueQuotedWithDelim() {
        Iterator<String> iter = QuoteUtil.splitAt("Key = \"Option=Value\"", "=");
        assertSplitAt(iter, "Key", "Option=Value");
    }

    @Test
    public void testSplitKeyValueSimple() {
        Iterator<String> iter = QuoteUtil.splitAt("Key=Value", "=");
        assertSplitAt(iter, "Key", "Value");
    }

    @Test
    public void testSplitKeyValueWithWhitespace() {
        Iterator<String> iter = QuoteUtil.splitAt("Key = Value", "=");
        assertSplitAt(iter, "Key", "Value");
    }

    @Test
    public void testQuoteIfNeeded() {
        StringBuilder buf = new StringBuilder();
        QuoteUtil.quoteIfNeeded(buf, "key", ",");
        assertEquals("key", buf.toString());
    }

    @Test
    public void testQuoteIfNeedednull() {
        StringBuilder buf = new StringBuilder();
        QuoteUtil.quoteIfNeeded(buf, null, ";=");
        assertEquals("", buf.toString());
    }
}
