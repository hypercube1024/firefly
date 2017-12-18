package test.http.router.handler;

import com.firefly.$;
import com.firefly.client.http2.*;
import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.*;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Phaser;

import static com.firefly.utils.io.BufferUtils.toBuffer;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestH2cUpgrade extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws Exception {
        Phaser phaser = new Phaser(5);
        HTTP2Server server = createServer();
        HTTP2Client client = createClient(phaser);

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }

    private static class TestH2cHandler extends ClientHTTPHandler.Adapter {

        protected final ByteBuffer[] buffers;
        protected final List<ByteBuffer> contentList = new CopyOnWriteArrayList<>();

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
            Assert.assertTrue(buffers != null);

            try (HTTPOutputStream out = output) {
                for (ByteBuffer buf : buffers) {
                    out.write(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response,
                               HTTPOutputStream output,
                               HTTPConnection connection) {
            System.out.println("client received data: " + BufferUtils.toUTF8String(item));
            contentList.add(item);
            return false;
        }
    }

    private HTTP2Client createClient(Phaser phaser) throws Exception {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
        HTTP2Client client = new HTTP2Client(http2Configuration);

        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
        client.connect(host, port, promise);

        final HTTPClientConnection httpConnection = promise.get();
        HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);
        FuturePromise<HTTP2ClientConnection> http2Promise = new FuturePromise<>();

        httpConnection.upgradeHTTP2(request, settingsFrame, http2Promise, new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("client received frame: " + response.getStatus() + ", " + response.getReason());
                System.out.println(response.getFields());
                System.out.println("---------------------------------");
                Assert.assertThat(response.getStatus(), is(HttpStatus.SWITCHING_PROTOCOLS_101));
                Assert.assertThat(response.getFields().get(HttpHeader.UPGRADE), is("h2c"));
                phaser.arrive(); // 1
                return true;
            }
        });

        HTTP2ClientConnection clientConnection = http2Promise.get();
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
                return dataComplete(phaser, BufferUtils.toString(contentList), response); // 2
            }
        });

        MetaData.Request get = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField(host + ":" + port),
                "/test2", HttpVersion.HTTP_1_1, new HttpFields());
        clientConnection.send(get, new TestH2cHandler() {
            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("client received frame: " + response.getStatus() + ", " + response.getReason());
                System.out.println(response.getFields());
                System.out.println("---------------------------------");
                Assert.assertThat(response.getStatus(), is(HttpStatus.NOT_FOUND_404));
                phaser.arrive(); // 3
                return true;
            }
        });

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
                return dataComplete(phaser, BufferUtils.toString(contentList), response); // 4
            }
        });

        return client;
    }

    public boolean dataComplete(Phaser phaser, String content, MetaData.Response response) {
        System.out.println("client received frame: " + response.getStatus() + ", " + response.getReason());
        System.out.println(response.getFields());
        System.out.println("---------------------------------");
        Assert.assertThat(response.getStatus(), is(HttpStatus.OK_200));
        Assert.assertThat(content, is("Receive data stream successful. Thank you!"));
        phaser.arrive();
        return true;
    }

    private HTTP2Server createServer() {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

        HTTP2Server server = new HTTP2Server(host, port, http2Configuration, new ServerHTTPHandler.Adapter() {

            @Override
            public boolean accept100Continue(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                             HTTPConnection connection) {
                System.out.println("received expect continue ");
                return false;
            }

            @Override
            public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                   HTTPConnection connection) {
                System.out.println("received data: " + BufferUtils.toString(item, StandardCharsets.UTF_8));
                return false;
            }

            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response, HTTPOutputStream outputStream,
                                           HTTPConnection connection) {
                HttpURI uri = request.getURI();
                System.out.println("message complete: " + uri);
                System.out.println(request.getFields());
                System.out.println("--------------------------------");
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
                            output.write(toBuffer("Receive data stream successful. ", StandardCharsets.UTF_8));
                            output.write(toBuffer("Thank you!", StandardCharsets.UTF_8));
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
                return true;
            }
        });
        server.start();
        return server;
    }

    @Test
    public void testLowLevelAPI() throws Exception {
        Phaser phaser = new Phaser(2);
        HTTP2Server server = createServerLowLevelAPI();
        HTTP2Client client = createClientLowLevelClient(phaser);

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }

    public HTTP2Server createServerLowLevelAPI() {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

        HTTP2Server server = new HTTP2Server(host, port, http2Configuration, new ServerSessionListener.Adapter() {

            @Override
            public Map<Integer, Integer> onPreface(Session session) {
                System.out.println("session preface: " + session);
                final Map<Integer, Integer> settings = new HashMap<>();
                settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
                settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
                return settings;
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame) {
                System.out.println("Server new stream, " + frame.getMetaData() + "|" + stream);

                MetaData metaData = frame.getMetaData();
                Assert.assertTrue(metaData.isRequest());
                final MetaData.Request request = (MetaData.Request) metaData;

                if (frame.isEndStream()) {
                    if (request.getURI().getPath().equals("/index")) {
                        MetaData.Response response = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                        HeadersFrame headersFrame = new HeadersFrame(stream.getId(), response, null, true);
                        stream.headers(headersFrame, Callback.NOOP);
                    }
                }

                List<ByteBuffer> contentList = new CopyOnWriteArrayList<>();

                return new Stream.Listener.Adapter() {

                    @Override
                    public void onHeaders(Stream stream, HeadersFrame frame) {
                        System.out.println("Server stream on headers " + frame.getMetaData() + "|" + stream);
                    }

                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback) {
                        System.out.println("Server stream on data: " + frame);
                        contentList.add(frame.getData());
                        if (frame.isEndStream()) {
                            MetaData.Response response = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                            HeadersFrame responseFrame = new HeadersFrame(stream.getId(), response, null, false);
                            System.out.println("Server session on data end: " + BufferUtils.toString(contentList));
                            stream.headers(responseFrame, new Callback() {
                                @Override
                                public void succeeded() {
                                    DataFrame dataFrame = new DataFrame(stream.getId(), BufferUtils.toBuffer("The server received data"), true);
                                    stream.data(dataFrame, Callback.NOOP);
                                }
                            });
                        }
                        callback.succeeded();
                    }
                };
            }

            @Override
            public void onAccept(Session session) {
                System.out.println("accept a new session " + session);
            }
        }, new ServerHTTPHandler.Adapter());
        server.start();
        return server;
    }

    public HTTP2Client createClientLowLevelClient(Phaser phaser) throws Exception {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
        HTTP2Client client = new HTTP2Client(http2Configuration);

        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
        client.connect(host, port, promise);

        HTTPConnection connection = promise.get();
        Assert.assertThat(connection.getHttpVersion(), is(HttpVersion.HTTP_1_1));

        final HTTP1ClientConnection httpConnection = (HTTP1ClientConnection) connection;
        HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);

        FuturePromise<HTTP2ClientConnection> http2promise = new FuturePromise<>();
        FuturePromise<Stream> initStream = new FuturePromise<>();
        httpConnection.upgradeHTTP2(request, settingsFrame, http2promise, initStream, new Stream.Listener.Adapter() {
            @Override
            public void onHeaders(Stream stream, HeadersFrame frame) {
                System.out.println($.string.replace("client stream {} received init headers: {}", stream.getId(), frame.getMetaData()));
            }

        }, new Session.Listener.Adapter() {

            @Override
            public Map<Integer, Integer> onPreface(Session session) {
                System.out.println($.string.replace("client preface: {}", session));
                Map<Integer, Integer> settings = new HashMap<>();
                settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
                settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
                return settings;
            }

            @Override
            public void onFailure(Session session, Throwable failure) {
                failure.printStackTrace();
            }
        }, new ClientHTTPHandler.Adapter());

        HTTP2ClientConnection clientConnection = http2promise.get();
        Assert.assertThat(clientConnection.getHttpVersion(), is(HttpVersion.HTTP_2));

        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.ACCEPT, "text/html");
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        fields.put(HttpHeader.CONTENT_LENGTH, "28");
        MetaData.Request metaData = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField(host + ":" + port), "/data", HttpVersion.HTTP_2, fields);

        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        clientConnection.getHttp2Session().newStream(new HeadersFrame(metaData, null, false), streamPromise,
                new Stream.Listener.Adapter() {

                    @Override
                    public void onHeaders(Stream stream, HeadersFrame frame) {
                        System.out.println($.string.replace("client received headers: {}", frame.getMetaData()));
                    }

                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback) {
                        System.out.println($.string.replace("client received data: {}, {}", BufferUtils.toUTF8String(frame.getData()), frame));
                        if (frame.isEndStream()) {
                            phaser.arrive(); // 1
                        }
                        callback.succeeded();
                    }
                });

        final Stream clientStream = streamPromise.get();
        System.out.println("client stream id: " + clientStream.getId());

        clientStream.data(new DataFrame(clientStream.getId(),
                toBuffer("hello world!", StandardCharsets.UTF_8), false), new Callback() {
            @Override
            public void succeeded() {
                clientStream.data(new DataFrame(clientStream.getId(),
                        toBuffer("big hello world!", StandardCharsets.UTF_8), true), Callback.NOOP);
            }
        });

        return client;
    }
}
