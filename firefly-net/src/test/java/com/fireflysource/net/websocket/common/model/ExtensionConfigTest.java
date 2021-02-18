package com.fireflysource.net.websocket.common.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionConfigTest {

    private void assertConfig(ExtensionConfig cfg, String expectedName, Map<String, String> expectedParams) {
        assertEquals(expectedName, cfg.getName());

        Map<String, String> actualParams = cfg.getParameters();
        assertNotNull(actualParams);
        assertEquals(expectedParams.size(), actualParams.size());

        for (String expectedKey : expectedParams.keySet()) {
            assertTrue(actualParams.containsKey(expectedKey));

            String expectedValue = expectedParams.get(expectedKey);
            String actualValue = actualParams.get(expectedKey);

            assertEquals(expectedValue, actualValue);
        }
    }

    @Test
    public void testParseMuxExample() {
        ExtensionConfig cfg = ExtensionConfig.parse("mux; max-channels=4; flow-control");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("max-channels", "4");
        expectedParams.put("flow-control", null);
        assertConfig(cfg, "mux", expectedParams);
    }

    @Test
    public void testParsePerMessageCompressExample1() {
        ExtensionConfig cfg = ExtensionConfig.parse("permessage-compress; method=foo");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("method", "foo");
        assertConfig(cfg, "permessage-compress", expectedParams);
    }

    @Test
    public void testParsePerMessageCompressExample2() {
        ExtensionConfig cfg = ExtensionConfig.parse("permessage-compress; method=\"foo; x=10\"");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("method", "foo; x=10");
        assertConfig(cfg, "permessage-compress", expectedParams);
    }

    @Test
    public void testParsePerMessageCompressExample3() {
        ExtensionConfig cfg = ExtensionConfig.parse("permessage-compress; method=\"foo, bar\"");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("method", "foo, bar");
        assertConfig(cfg, "permessage-compress", expectedParams);
    }

    @Test
    public void testParsePerMessageCompressExample4() {
        ExtensionConfig cfg = ExtensionConfig.parse("permessage-compress; method=\"foo; use_x, foo\"");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("method", "foo; use_x, foo");
        assertConfig(cfg, "permessage-compress", expectedParams);
    }

    @Test
    public void testParsePerMessageCompressExample5() {
        ExtensionConfig cfg = ExtensionConfig.parse("permessage-compress; method=\"foo; x=\\\"Hello World\\\", bar\"");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("method", "foo; x=\"Hello World\", bar");
        assertConfig(cfg, "permessage-compress", expectedParams);
    }

    @Test
    public void testParseSimpleBasicParameters() {
        ExtensionConfig cfg = ExtensionConfig.parse("bar; baz=2");
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("baz", "2");
        assertConfig(cfg, "bar", expectedParams);
    }

    @Test
    public void testParseSimpleNoParameters() {
        ExtensionConfig cfg = ExtensionConfig.parse("foo");
        Map<String, String> expectedParams = new HashMap<>();
        assertConfig(cfg, "foo", expectedParams);
    }
}
