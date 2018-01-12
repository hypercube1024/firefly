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
    @Override
    public void test() throws Exception {
        testServerAndClient(Collections.singletonList("deflate-frame"));
    }
}
