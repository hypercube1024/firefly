package test.websocket;

import com.firefly.$;
import com.firefly.client.websocket.SimpleWebSocketClient;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.RandomUtils;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author Pengtao Qiu
 */
public class TestWebSocket {

    private final String host = "localhost";
    private int port = (int) RandomUtils.random(3000, 65534);

    @Test
    public void test() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        HTTP2ServerBuilder server = $.httpServer();
        server.websocket("/helloWebSocket")
              .onConnect(conn -> conn.sendText("OK"))
              .onText((text, conn) -> {
                  System.out.println("Server received: " + text);
                  latch.countDown();
              })
              .listen(host, port);

        SimpleWebSocketClient client = $.websocketClient();
        client.url("http://" + host + ":" + port + "/helloWebSocket")
              .onText((text, conn) -> {
                  System.out.println("Client received: " + text);
                  latch.countDown();
              })
              .connect()
              .thenAccept(conn -> conn.sendText("Hello Websocket"));

        latch.await();
        server.stop();
        client.stop();
    }
}
