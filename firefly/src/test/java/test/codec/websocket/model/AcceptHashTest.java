package test.codec.websocket.model;

import com.firefly.codec.websocket.model.AcceptHash;
import com.firefly.utils.codec.B64Code;
import com.firefly.utils.lang.TypeUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

public class AcceptHashTest {
    @Test
    public void testHash() {
        byte key[] = TypeUtils.fromHexString("00112233445566778899AABBCCDDEEFF");
        Assert.assertThat("Key size", key.length, is(16));

        // what the client sends
        String clientKey = String.valueOf(B64Code.encode(key));
        // what the server responds with
        String serverHash = AcceptHash.hashKey(clientKey);

        // how the client validates
        Assert.assertThat(serverHash, is("mVL6JKtNRC4tluIaFAW2hhMffgE="));
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

        Assert.assertThat(serverAccept, is(expectedHash));
    }
}
