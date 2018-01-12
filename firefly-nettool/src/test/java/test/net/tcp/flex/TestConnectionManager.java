package test.net.tcp.flex;

import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.client.MultiplexingClient;
import com.firefly.net.tcp.flex.client.MultiplexingClientConfiguration;
import com.firefly.net.tcp.flex.server.MultiplexingServer;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.HostPort;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestConnectionManager {

    @Test
    public void test() throws InterruptedException {
        int loop = 10;
        CountDownLatch latch = new CountDownLatch(loop);
        List<HostPort> addresses = createAddresses(1);
        List<MultiplexingServer> servers = addresses.stream()
                                                    .map(a -> createServer(a.getHost(), a.getPort()))
                                                    .collect(Collectors.toList());

        MultiplexingClientConfiguration configuration = new MultiplexingClientConfiguration();
        configuration.setServerUrlSet(addresses.stream()
                                               .map(a -> a.getHost() + ":" + a.getPort())
                                               .collect(Collectors.toSet()));
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
                    latch.countDown();
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
            System.out.println("Send request " + i + " complete");
        }

        latch.await();
        servers.forEach(AbstractLifeCycle::stop);
        client.stop();
    }

    public List<HostPort> createAddresses(int number) {
        return IntStream.range(0, number).boxed()
                        .map(i -> new HostPort("localhost:" + (int) RandomUtils.random(1000, 65534)))
                        .collect(Collectors.toList());
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
