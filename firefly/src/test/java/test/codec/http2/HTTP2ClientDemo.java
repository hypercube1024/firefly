package test.codec.http2;

import com.firefly.client.http2.ClientHTTP2SessionListener;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTP2ClientConnection;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class HTTP2ClientDemo {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, UnsupportedEncodingException {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setSecureConnectionEnabled(true);
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
        HTTP2Client client = new HTTP2Client(http2Configuration);

        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
        client.connect("127.0.0.1", 6677, promise, new ClientHTTP2SessionListener() {

            @Override
            public Map<Integer, Integer> onPreface(Session session) {
                log.info("client preface: {}", session);
                Map<Integer, Integer> settings = new HashMap<>();
                settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
                settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
                return settings;
            }

            @Override
            public com.firefly.codec.http2.stream.Stream.Listener onNewStream(Stream stream, HeadersFrame frame) {
                return null;
            }

            @Override
            public void onSettings(Session session, SettingsFrame frame) {
                log.info("client received settings frame: {}", frame);
            }

            @Override
            public void onPing(Session session, PingFrame frame) {
            }

            @Override
            public void onReset(Session session, ResetFrame frame) {
                log.info("client resets {}", frame);
            }

            @Override
            public void onClose(Session session, GoAwayFrame frame) {
                log.info("client is closed {}", frame);
            }

            @Override
            public void onFailure(Session session, Throwable failure) {
                log.error("client failure, {}", failure, session);
            }

            @Override
            public boolean onIdleTimeout(Session session) {
                return false;
            }
        });

        HTTPConnection connection = promise.get();
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.ACCEPT, "text/html");
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        fields.put(HttpHeader.CONTENT_LENGTH, "28");
        MetaData.Request metaData = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField("127.0.0.1:6677"), "/data", HttpVersion.HTTP_2, fields);

        HTTP2ClientConnection clientConnection = (HTTP2ClientConnection) connection;

        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        clientConnection.getHttp2Session().newStream(new HeadersFrame(metaData, null, false), streamPromise,
                new Stream.Listener() {

                    @Override
                    public void onHeaders(Stream stream, HeadersFrame frame) {
                        log.info("client received headers: {}", frame);
                    }

                    @Override
                    public com.firefly.codec.http2.stream.Stream.Listener onPush(Stream stream,
                                                                                 PushPromiseFrame frame) {
                        return null;
                    }

                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback) {
                        log.info("client received data: {}, {}", BufferUtils.toUTF8String(frame.getData()), frame);
                        callback.succeeded();
                    }

                    @Override
                    public void onReset(Stream stream, ResetFrame frame) {
                        log.info("client reset: {}, {}", stream, frame);
                    }

                    @Override
                    public boolean onIdleTimeout(Stream stream, Throwable x) {
                        log.error("the client stream {} is timeout", x, stream);
                        return true;
                    }


                });

        final Stream clientStream = streamPromise.get();
        log.info("client stream id is ", clientStream.getId());

        final DataFrame smallDataFrame = new DataFrame(clientStream.getId(),
                ByteBuffer.wrap("hello world!".getBytes("UTF-8")), false);
        final DataFrame bigDataFrame = new DataFrame(clientStream.getId(),
                ByteBuffer.wrap("big hello world!".getBytes("UTF-8")), true);

        clientStream.data(smallDataFrame, new Callback() {

            @Override
            public void succeeded() {
                log.info("client sents small data success");
                clientStream.data(bigDataFrame, new Callback() {

                    @Override
                    public void succeeded() {
                        log.info("client sents big data success");

                    }

                    @Override
                    public void failed(Throwable x) {
                        log.info("client sents big data failure");
                    }
                });
            }

            @Override
            public void failed(Throwable x) {
                log.info("client sents small data failure");
            }
        });

    }
}
