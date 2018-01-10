package test.websocket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestDeflatedExtension extends TestWebSocket {

    @Test
    public void testDeflate() throws InterruptedException {
        testServerAndClient(Collections.singletonList("deflate-frame"));
    }
}
