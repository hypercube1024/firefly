package test.http.router.handler;

import com.firefly.client.http2.*;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.firefly.utils.io.BufferUtils.toBuffer;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestH2cUpgrade extends AbstractHTTPHandlerTest {

    private int timeout = 3 * 1000;
    private int corePoolSize = 4;

    @Test
    public void test() throws Exception {
        HTTP2Server server = createServer();
        HTTP2Client client = createClient();

        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
        client.connect(host, port, promise);

        final HTTPClientConnection httpConnection = promise.get();
        final HTTP2ClientConnection clientConnection = upgradeHttp2(client.getHttp2Configuration(), httpConnection);

        clientConnection.close(c -> System.out.println("The client connection closed."))
                        .exception((c, ex) -> ex.printStackTrace());

        int loop = 5;
        CountDownLatch latch = new CountDownLatch(loop);
        for (int j = 0; j < loop; j++) {
            sendData(latch, clientConnection);
        }

        CountDownLatch latch2 = new CountDownLatch(loop);
        for (int i = 0; i < loop; i++) {
            sendDataWithContinuation(latch2, clientConnection);
        }

        CountDownLatch latch3 = new CountDownLatch(loop);
        for (int i = 0; i < loop; i++) {
            test404(latch3, clientConnection);
        }

        try {
            latch.await(3, TimeUnit.SECONDS);
            latch2.await(3, TimeUnit.SECONDS);
            latch3.await(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Completed all tasks.");
        server.stop();
        client.stop();
    }

    private static class TestH2cHandler extends ClientHTTPHandler.Adapter {

        protected final ByteBuffer[] buffers;
        protected final List<ByteBuffer> contentList = new ArrayList<>();

        public TestH2cHandler() {
            buffers = null;
        }

        public TestH2cHandler(ByteBuffer[] buffers) {
            this.buffers = buffers;
        }

        @Override
        public void continueToSendData(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                       HTTPConnection connection) {
            System.out.println("client received 100 continue");
            if (buffers != null) {
                System.out.println("buffers: " + buffers.length);
                try (HTTPOutputStream out = output) {
                    for (ByteBuffer buf : buffers) {
                        out.write(buf);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("client sends buffers completely");
            }
        }

        @Override
        public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response,
                               HTTPOutputStream output,
                               HTTPConnection connection) {
//            System.out.println("client received data: " + BufferUtils.toUTF8String(item));
            contentList.add(item);
            return false;
        }

        @Override
        public void badMessage(int status, String reason, MetaData.Request request, MetaData.Response response,
                               HTTPOutputStream output, HTTPConnection connection) {
            System.out.println("Client received the bad message. " + reason);
        }

        @Override
        public void earlyEOF(MetaData.Request request, MetaData.Response response,
                             HTTPOutputStream output,
                             HTTPConnection connection) {
            System.out.println("Client is early EOF. ");
        }

    }

    private HTTP2Client createClient() {
        final HTTP2Configuration config = new HTTP2Configuration();
        config.getTcpConfiguration().setTimeout(timeout);
        config.getTcpConfiguration().setAsynchronousCorePoolSize(corePoolSize);
        return new HTTP2Client(config);
    }

    private HTTP2ClientConnection upgradeHttp2(HTTP2Configuration http2Configuration, HTTPClientConnection httpConnection) throws Exception {
        HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);

        FuturePromise<HTTP2ClientConnection> http2Promise = new FuturePromise<>();

        ClientHTTPHandler upgradeHandler = new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                printResponse(request, response, BufferUtils.toString(contentList));
                Assert.assertThat(response.getStatus(), is(HttpStatus.SWITCHING_PROTOCOLS_101));
                Assert.assertThat(response.getFields().get(HttpHeader.UPGRADE), is("h2c"));
                return true;
            }
        };

        ClientHTTPHandler h2ResponseHandler = new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("Client received init status: " + response.getStatus());
                String content = BufferUtils.toString(contentList);
                printResponse(request, response, content);
                Assert.assertThat(response.getStatus(), is(HttpStatus.OK_200));
                Assert.assertThat(content, is("receive initial stream successful"));
                return true;
            }
        };

        httpConnection.upgradeHTTP2(request, settingsFrame, http2Promise, upgradeHandler, h2ResponseHandler);
        System.out.println("get the h2 connection");
        return http2Promise.get();
    }

    private void test404(CountDownLatch latch, HTTP2ClientConnection clientConnection) {
        System.out.println("Client test 404.");
        MetaData.Request get = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField(host + ":" + port),
                "/test2", HttpVersion.HTTP_1_1, new HttpFields());
        clientConnection.send(get, new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                printResponse(request, response, BufferUtils.toString(contentList));
                Assert.assertThat(response.getStatus(), is(HttpStatus.NOT_FOUND_404));
                latch.countDown();
                System.out.println("Remain task: " + latch.getCount());
                return true;
            }
        });
    }

    private void sendData(CountDownLatch latch, HTTP2ClientConnection clientConnection) throws UnsupportedEncodingException {
        System.out.println("Client sends data.");
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        MetaData.Request post2 = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField(host + ":" + port),
                "/data", HttpVersion.HTTP_1_1, fields);
        clientConnection.send(post2, new ByteBuffer[]{
                ByteBuffer.wrap("test data 2".getBytes("UTF-8")),
                ByteBuffer.wrap("finished test data 2".getBytes("UTF-8"))}, new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                return dataComplete(latch, BufferUtils.toString(contentList), request, response);
            }
        });
    }

    private void sendDataWithContinuation(CountDownLatch latch, HTTP2ClientConnection clientConnection) throws UnsupportedEncodingException {
        System.out.println("Client sends data with continuation");
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        MetaData.Request post = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField(host + ":" + port),
                "/data", HttpVersion.HTTP_1_1, fields);
        clientConnection.sendRequestWithContinuation(post, new TestH2cHandler(new ByteBuffer[]{
                ByteBuffer.wrap("hello world!".getBytes("UTF-8")),
                ByteBuffer.wrap("big hello world!".getBytes("UTF-8"))}) {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                return dataComplete(latch, BufferUtils.toString(contentList), request, response);
            }
        });
    }

    private void printResponse(MetaData.Request request, MetaData.Response response, String content) {
        System.out.println("client---------------------------------");
        System.out.println("client received frame: " + request + ", " + response);
        System.out.println(response.getFields());
        System.out.println(content);
        System.out.println("client---------------------------------end");
        System.out.println();
    }

    public boolean dataComplete(CountDownLatch latch, String content, MetaData.Request request, MetaData.Response response) {
        printResponse(request, response, content);
        Assert.assertThat(response.getStatus(), is(HttpStatus.OK_200));
        Assert.assertThat(content, is("Receive data stream successful. Thank you!"));
        latch.countDown();
        System.out.println("Remain task: " + latch.getCount());
        return true;
    }

    private HTTP2Server createServer() {
        final HTTP2Configuration config = new HTTP2Configuration();
        config.getTcpConfiguration().setTimeout(timeout);
        config.getTcpConfiguration().setAsynchronousCorePoolSize(corePoolSize);
        HTTP2Server server = new HTTP2Server(host, port, config, new ServerHTTPHandler.Adapter() {

            @Override
            public void badMessage(int status, String reason, MetaData.Request request, MetaData.Response response,
                                   HTTPOutputStream output, HTTPConnection connection) {
                System.out.println("Server received the bad message. " + reason);
            }

            @Override
            public void earlyEOF(MetaData.Request request, MetaData.Response response,
                                 HTTPOutputStream output,
                                 HTTPConnection connection) {
                System.out.println("Server is early EOF. ");
            }

            @Override
            public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                   HTTPConnection connection) {
//                System.out.println("Server received data: " + BufferUtils.toString(item, StandardCharsets.UTF_8));
                return false;
            }

            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response, HTTPOutputStream outputStream,
                                           HTTPConnection connection) {
                System.out.println("Server received request: " + request + ", " + outputStream.getClass() + ", " + connection.getHttpVersion());
                HttpURI uri = request.getURI();
                switch (uri.getPath()) {
                    case "/index":
                        response.setStatus(HttpStatus.Code.OK.getCode());
                        response.setReason(HttpStatus.Code.OK.getMessage());
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(toBuffer("receive initial stream successful", StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "/data":
                        response.setStatus(HttpStatus.Code.OK.getCode());
                        response.setReason(HttpStatus.Code.OK.getMessage());
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(new ByteBuffer[]{
                                    toBuffer("Receive data stream successful. ", StandardCharsets.UTF_8),
                                    toBuffer("Thank you!", StandardCharsets.UTF_8)
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        response.setStatus(HttpStatus.Code.NOT_FOUND.getCode());
                        response.setReason(HttpStatus.Code.NOT_FOUND.getMessage());
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(toBuffer(uri.getPath() + " not found", StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
//                System.out.println("server--------------------------------end");
                return true;
            }
        });
        server.start();
        return server;
    }

}
