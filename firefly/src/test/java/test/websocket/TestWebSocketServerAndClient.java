package test.websocket;

import org.junit.Test;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
public class TestWebSocketServerAndClient extends TestWebSocket {

    @Test
    public void test() throws InterruptedException {
        _test(Collections.emptyList());
    }
}
