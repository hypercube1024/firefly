package test.websocket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestWebSocketServerAndClient extends TestWebSocket {

    @Test
    public void test() throws InterruptedException {
        testServerAndClient(Collections.emptyList());
    }
}
