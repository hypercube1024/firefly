package test.http.router.handler;

import com.firefly.$;
import com.firefly.client.http2.*;
import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

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
public class TestH2cLowLevelAPI extends AbstractHTTPHandlerTest {
    
    @Test
    public void testLowLevelAPI() throws Exception {
        Phaser phaser = new Phaser(2);
        HTTP2Server server = createServerLowLevelAPI();
        HTTP2Client client = createClientLowLevelClient(phaser);

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

        for (int i = 0; i < 1; i++) {
            testReq(phaser, clientConnection);
        }
        return client;
    }

    private void testReq(Phaser phaser, HTTP2ClientConnection clientConnection) throws InterruptedException, java.util.concurrent.ExecutionException {
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
        phaser.arriveAndAwaitAdvance();
    }
}
