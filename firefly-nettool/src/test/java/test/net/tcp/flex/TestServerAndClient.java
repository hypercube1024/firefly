package test.net.tcp.flex;

import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.client.MultiplexingClient;
import com.firefly.net.tcp.flex.server.MultiplexingServer;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestServerAndClient {

    @Test
    public void testClientRequest() {
        Phaser phaser = new Phaser(2);
        String host = "localhost";
        int port = (int) RandomUtils.random(1000, 65534);

        MultiplexingServer server = new MultiplexingServer();
        server.accept(connection -> connection.onRequest(new FlexConnection.Listener() {

            @Override
            public void newRequest(Context context) {
                System.out.println("Server received the new request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
                Assert.assertThat(context.getRequest().getPath(), is("flex://hello"));
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
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                System.out.println("Server message complete: " + data);
                Assert.assertThat(data, is("Array [0,1,2,3,4,5,6,7,8,9,]"));

                try (PrintWriter writer = context.getPrintWriter()) {
                    writer.write("Server received array");
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

        MultiplexingClient client = new MultiplexingClient();
        client.connect(host, port).thenAccept(connection -> {
            Request request = new Request();
            request.setPath("flex://hello");
            connection.newRequest(request, new FlexConnection.Listener() {
                @Override
                public void newRequest(Context context) {
                    System.out.println("Client on new request and send data");
                    try (PrintWriter writer = context.getPrintWriter()) {
                        writer.write("Array [");
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
                    Assert.assertThat(data, is("Server received array"));
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
        });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }

    @Test
    public void testServerPush() {
        Phaser phaser = new Phaser(2);
        String host = "localhost";
        int port = (int) RandomUtils.random(1000, 65534);

        MultiplexingServer server = new MultiplexingServer();
        server.accept(connection -> {
            Request request = new Request();
            request.setPath("flex://serverPush");
            connection.newRequest(request, new FlexConnection.Listener() {
                @Override
                public void newRequest(Context context) {
                    System.out.println("Server push new request and send data");
                    try (PrintWriter writer = context.getPrintWriter()) {
                        writer.write("Push Array [");
                        for (int i = 0; i < 10; i++) {
                            writer.write((i + ","));
                        }
                        writer.write("]");
                    }
                }

                @Override
                public void newResponse(Context context) {
                    System.out.println("Server received response: " + context.getResponse());
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
                    System.out.println("Server message complete: " + data);
                    Assert.assertThat(context.getResponse().getMessage(), is("OK"));
                    Assert.assertThat(data, is("Client received array"));
                    phaser.arrive();
                }

                @Override
                public void close(Context context) {
                    System.out.println("Server stream " + context.getStream().getId() + " closed");
                }

                @Override
                public void exception(Context context, Throwable t) {
                    t.printStackTrace();
                }
            });
        }).listen(host, port);

        MultiplexingClient client = new MultiplexingClient();
        client.accept(connection -> connection.onRequest(new FlexConnection.Listener() {

            @Override
            public void newRequest(Context context) {
                System.out.println("Client received the request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
                Assert.assertThat(context.getRequest().getPath(), is("flex://serverPush"));
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
                context.getResponse().getFields().put("Client", "flex v1");
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                System.out.println("Client message complete: " + data);
                Assert.assertThat(data, is("Push Array [0,1,2,3,4,5,6,7,8,9,]"));

                try (PrintWriter writer = context.getPrintWriter()) {
                    writer.write("Client received array");
                }
            }

            @Override
            public void close(Context context) {
                System.out.println("Client stream " + context.getStream().getId() + " closed");
            }

            @Override
            public void exception(Context context, Throwable t) {
                t.printStackTrace();
            }
        })).connect(host, port);

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }

}
