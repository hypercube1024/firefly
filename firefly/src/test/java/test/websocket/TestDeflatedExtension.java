package test.websocket;

import org.junit.Test;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
public class TestDeflatedExtension extends TestWebSocket {

    @Test
    public void testDeflate() throws InterruptedException {
        _test(Collections.singletonList("deflate-frame"));
    }
}
