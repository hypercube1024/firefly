package com.fireflysource.net.websocket.common.model;

import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AcceptHashTest {

    @Test
    public void testHash() {
        byte[] key = TypeUtils.fromHexString("00112233445566778899AABBCCDDEEFF");
        assertEquals(16, key.length);

        // what the client sends
        String clientKey = Base64.getEncoder().encodeToString(key);
        // what the server responds with
        String serverHash = AcceptHash.hashKey(clientKey);

        // how the client validates
        assertEquals("mVL6JKtNRC4tluIaFAW2hhMffgE=", serverHash);
    }

    /**
     * Test of values present in RFC-6455.
     * <p>
     * Note: client key bytes are "7468652073616d706c65206e6f6e6365"
     */
    @Test
    public void testRfcHashExample() {
        // What the client sends in the RFC
        String clientKey = "dGhlIHNhbXBsZSBub25jZQ==";

        // What the server responds with
        String serverAccept = AcceptHash.hashKey(clientKey);
        String expectedHash = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";

        assertEquals(expectedHash, serverAccept);
    }
}
