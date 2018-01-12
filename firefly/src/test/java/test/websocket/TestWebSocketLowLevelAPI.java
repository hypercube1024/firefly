package test.websocket;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.client.http2.HTTPClientRequest;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.TextFrame;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.WebSocketHandler;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.concurrent.FuturePromise;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;


/**
 * @author Pengtao Qiu
 */
public class TestWebSocketLowLevelAPI {

    private String host = "localhost";
    private int port = (int) RandomUtils.random(3000, 65534);

    @Test
    public void test() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        HTTP2Server server = createServer(latch);
        HTTP2Client client = new HTTP2Client(new HTTP2Configuration());

        HTTPClientConnection connection = client.connect(host, port).get();
        HTTPClientRequest request = new HTTPClientRequest("GET", "/index");
        FuturePromise<WebSocketConnection> promise = new FuturePromise<>();

        connection.upgradeWebSocket(request, WebSocketPolicy.newClientPolicy(), promise, new ClientHTTPHandler.Adapter() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("upgrade websocket success: " + response);
                return true;
            }
        }, new IncomingFrames() {
            @Override
            public void incomingError(Throwable t) {

            }

            @Override
            public void incomingFrame(Frame frame) {
                switch (frame.getType()) {
                    case TEXT: {
                        TextFrame textFrame = (TextFrame) frame;
                        System.out.println("Client received: " + textFrame + ", " + textFrame.getPayloadAsUTF8());
                        Assert.assertThat(textFrame.getPayloadAsUTF8(), is("OK"));
                        latch.countDown();
                    }
                }
            }
        });

        WebSocketConnection webSocketConnection = promise.get();
        webSocketConnection.sendText("Hello WebSocket").thenAccept(r -> System.out.println("Client sends text frame success."));

        latch.await(5, TimeUnit.SECONDS);
        server.stop();
        client.stop();
    }


    public HTTP2Server createServer(CountDownLatch latch) {
        HTTP2Server server = new HTTP2Server(host, port, new HTTP2Configuration(), new ServerHTTPHandler.Adapter() {

            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                return true;
            }
        }, new WebSocketHandler() {

            @Override
            public void onConnect(WebSocketConnection webSocketConnection) {
                webSocketConnection.sendText("OK").thenAccept(r -> System.out.println("Server sends text frame success."));
            }

            @Override
            public void onFrame(Frame frame, WebSocketConnection connection) {
                switch (frame.getType()) {
                    case TEXT: {
                        TextFrame textFrame = (TextFrame) frame;
                        System.out.println("Server received: " + textFrame + ", " + textFrame.getPayloadAsUTF8());
                        Assert.assertThat(textFrame.getPayloadAsUTF8(), is("Hello WebSocket"));
                        latch.countDown();
                    }
                }

            }
        });
        server.start();
        return server;
    }
}
