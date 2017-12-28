package test.net.tcp.flex;

import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.client.MultiplexingClient;
import com.firefly.net.tcp.flex.client.MultiplexingClientConfiguration;
import com.firefly.net.tcp.flex.server.MultiplexingServer;
import com.firefly.net.tcp.flex.server.MultiplexingServerConfiguration;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.io.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class IdleStreamDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 11334;

        MultiplexingServer server = createServer(host, port);
        MultiplexingClientConfiguration configuration = new MultiplexingClientConfiguration();
        configuration.setStreamMaxIdleTime(1000);
        MultiplexingClient client = new MultiplexingClient(configuration);
        client.start();

        Request request = new Request();
        request.setPath("/testStreamTimeout");
        client.connect(host, port).thenAccept(connection -> connection.newRequest(request, new FlexConnection.Listener() {
            @Override
            public void newRequest(Context context) {
                System.out.println("Client on new request and send data");
                try (PrintWriter writer = context.getPrintWriter()) {
                    writer.write("ok");
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
            }
        }));
    }



    public static MultiplexingServer createServer(String host, int port) {
        MultiplexingServerConfiguration configuration = new MultiplexingServerConfiguration();
        configuration.setStreamMaxIdleTime(1000);
        MultiplexingServer server = new MultiplexingServer(configuration);
        server.accept(connection -> connection.onRequest(new FlexConnection.Listener() {
            @Override
            public void newRequest(Context context) {
                System.out.println("Server received the new request: " + context.getRequest());
                context.setAttribute("data", new ByteArrayOutputStream());
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
                System.out.println("...too slow");
                ThreadUtils.sleep(5, TimeUnit.SECONDS);
                try (PrintWriter writer = context.getPrintWriter()) {
                    writer.write("Server received message");
                }
            }
        })).listen(host, port);
        return server;
    }
}
