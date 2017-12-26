package test.net.tcp.flex;

import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.client.MultiplexingClient;
import com.firefly.net.tcp.flex.client.MultiplexingClientConfiguration;
import com.firefly.net.tcp.flex.server.MultiplexingServer;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestConnectionManager {

    @Test
    public void test() {
        int loop = 10;
        Phaser phaser = new Phaser(loop + 1);
        String host = "localhost";
        int port1 = (int) RandomUtils.random(1000, 65534);
        int port2 = (int) RandomUtils.random(1000, 65534);
        int port3 = (int) RandomUtils.random(1000, 65534);

        MultiplexingServer server1 = createServer(host, port1);
        MultiplexingServer server2 = createServer(host, port2);
        MultiplexingServer server3 = createServer(host, port3);


        MultiplexingClientConfiguration configuration = new MultiplexingClientConfiguration();
        configuration.setServerUrlSet(new HashSet<>(Arrays.asList(getUrl(host, port1), getUrl(host, port2), getUrl(host, port3))));
        MultiplexingClient client = new MultiplexingClient(configuration);
        client.start();

        for (int i = 0; i < loop; i++) {
            Request request = new Request();
            request.setPath("/connectionManager");
            request.setFields(new HashMap<>());
            request.getFields().put("taskNo", "req" + i);
            FlexConnection connection = client.getConnection();
            System.out.println(connection.getLocalAddress());
            connection.newRequest(request, new FlexConnection.Listener() {
                @Override
                public void newRequest(Context context) {
                    System.out.println("Client on new request and send data");
                    try (PrintWriter writer = context.getPrintWriter()) {
                        writer.write("Data [");
                        for (int i = 0; i < 10; i++) {
                            writer.write((i + ","));
                        }
                        writer.write("]");
                    }
                }

                @Override
                public void newResponse(Context context) {
                    System.out.println("Client received response: " + context.getResponse());
                    context.setAttribute("data", new ByteArrayOutputStream());
                }

                @Override
                public void content(Context context, byte[] receivedData) {
                    ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                    try {
                        out.write(receivedData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void contentComplete(Context context) {
                    ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                    IO.close(out);
                }

                @Override
                public void messageComplete(Context context) {
                    ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                    String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                    System.out.println("Client message complete: " + data);
                    Assert.assertThat(context.getResponse().getMessage(), is("OK"));
                    Assert.assertThat(data, is("Server received message"));
                    Assert.assertThat(context.getResponse().getFields().get("taskNo"), is(context.getRequest().getFields().get("taskNo")));
                    phaser.arrive();
                }

                @Override
                public void close(Context context) {
                    System.out.println("Client stream " + context.getStream().getId() + " closed");
                }

                @Override
                public void exception(Context context, Throwable t) {
                    t.printStackTrace();
                }
            });
        }

        phaser.arriveAndAwaitAdvance();
        server1.stop();
        server2.stop();
        server3.stop();
        client.stop();
    }



    private String getUrl(String host, int port) {
        return host + ":" + port;
    }

    public MultiplexingServer createServer(String host, int port) {
        MultiplexingServer server = new MultiplexingServer();
        server.accept(connection -> connection.onRequest(new FlexConnection.Listener() {

            @Override
            public void newRequest(Context context) {
                System.out.println("Server received the new request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
                Assert.assertThat(context.getRequest().getPath(), is("/connectionManager"));
            }

            @Override
            public void newResponse(Context context) {

            }

            @Override
            public void content(Context context, byte[] receivedData) {
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                try {
                    out.write(receivedData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void contentComplete(Context context) {
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                IO.close(out);
            }

            @Override
            public void messageComplete(Context context) {
                context.getResponse().setMessage("OK");
                context.getResponse().setFields(new HashMap<>());
                context.getResponse().getFields().put("Server", "flex v1");
                context.getResponse().getFields().put("taskNo", context.getRequest().getFields().get("taskNo"));
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                System.out.println("Server message complete: " + data);
                Assert.assertThat(data, is("Data [0,1,2,3,4,5,6,7,8,9,]"));

                try (PrintWriter writer = context.getPrintWriter()) {
                    writer.write("Server received message");
                }
            }

            @Override
            public void close(Context context) {
                System.out.println("Server stream " + context.getStream().getId() + " closed");
            }

            @Override
            public void exception(Context context, Throwable t) {
                t.printStackTrace();
            }
        })).listen(host, port);
        return server;
    }
}
