package test.net.tcp.ffsocks;

import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.stream.Context;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.ffsocks.client.FfsocksClient;
import com.firefly.net.tcp.ffsocks.server.FfsocksServer;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

        FfsocksServer server = new FfsocksServer();
        server.accept(connection -> connection.onRequest(new FfsocksConnection.Listener() {

            @Override
            public void newRequest(Context context) {
                System.out.println("Server received the new request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
                Assert.assertThat(context.getRequest().getPath(), is("ffsocks://hello"));
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
                context.getResponse().getFields().put("Server", "ffsocks v1");
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                System.out.println("Server message complete: " + data);
                Assert.assertThat(data, is("Array [0,1,2,3,4,5,6,7,8,9,]"));

                try (OutputStream outputStream = context.getOutputStream()) {
                    outputStream.write("Server received array".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        })).listen(host, port);

        FfsocksClient client = new FfsocksClient();
        client.connect(host, port).thenAccept(connection -> {
            Request request = new Request();
            request.setPath("ffsocks://hello");
            connection.newRequest(request, new FfsocksConnection.Listener() {
                @Override
                public void newRequest(Context context) {
                    System.out.println("Client on new request and send data");
                    try (OutputStream outputStream = context.getOutputStream()) {
                        outputStream.write("Array [".getBytes(StandardCharsets.UTF_8));
                        for (int i = 0; i < 10; i++) {
                            outputStream.write((i + ",").getBytes(StandardCharsets.UTF_8));
                        }
                        outputStream.write("]".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
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

        FfsocksServer server = new FfsocksServer();
        server.accept(connection -> {
            Request request = new Request();
            request.setPath("ffsocks://serverPush");
            connection.newRequest(request, new FfsocksConnection.Listener() {
                @Override
                public void newRequest(Context context) {
                    System.out.println("Server push new request and send data");
                    try (OutputStream outputStream = context.getOutputStream()) {
                        outputStream.write("Push Array [".getBytes(StandardCharsets.UTF_8));
                        for (int i = 0; i < 10; i++) {
                            outputStream.write((i + ",").getBytes(StandardCharsets.UTF_8));
                        }
                        outputStream.write("]".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
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
            });
        }).listen(host, port);

        FfsocksClient client = new FfsocksClient();
        client.connect(host, port).thenAccept(connection -> connection.onRequest(new FfsocksConnection.Listener() {

            @Override
            public void newRequest(Context context) {
                System.out.println("Client received the request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
                Assert.assertThat(context.getRequest().getPath(), is("ffsocks://serverPush"));
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
                context.getResponse().getFields().put("Client", "ffsocks v1");
                ByteArrayOutputStream out = (ByteArrayOutputStream) context.getAttribute("data");
                String data = new String(out.toByteArray(), StandardCharsets.UTF_8);
                System.out.println("Client message complete: " + data);
                Assert.assertThat(data, is("Push Array [0,1,2,3,4,5,6,7,8,9,]"));

                try (OutputStream outputStream = context.getOutputStream()) {
                    outputStream.write("Client received array".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }

}
