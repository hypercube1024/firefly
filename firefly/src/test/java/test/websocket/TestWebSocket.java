package test.websocket;

import com.firefly.$;
import com.firefly.client.websocket.SimpleWebSocketClient;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.BufferUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * @author Pengtao Qiu
 */
public class TestWebSocket {

    @Test
    public void test() throws Exception {
        String host = "localhost";
        int port = (int) RandomUtils.random(3000, 65534);
        int count = 20;

        CountDownLatch latch = new CountDownLatch(count * 2 + 1);
        HTTP2ServerBuilder server = $.httpServer();
        server.websocket("/helloWebSocket")
              .onConnect(conn -> {
                  for (int i = 0; i < count; i++) {
                      conn.sendText("Msg: " + i);
                      conn.sendData(("Data: " + i).getBytes(StandardCharsets.UTF_8));
                  }
              })
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
              .onData((buf, conn) -> {
                  System.out.println("Client received: " + BufferUtils.toString(buf));
                  latch.countDown();
              })
              .connect()
              .thenAccept(conn -> conn.sendText("Hello Websocket"));

        latch.await();
        server.stop();
        client.stop();
    }
}
