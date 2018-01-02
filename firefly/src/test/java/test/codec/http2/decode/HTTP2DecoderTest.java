package test.codec.http2.decode;

import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.encode.HeadersGenerator;
import com.firefly.codec.http2.encode.SettingsGenerator;
import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.server.http2.HTTP2ServerConnection;
import com.firefly.server.http2.HTTP2ServerDecoder;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.utils.concurrent.Callback;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class HTTP2DecoderTest {

    @Test
    public void testData() throws Throwable {
        final byte[] smallContent = new byte[22];
        final byte[] bigContent = new byte[50];
        Random random = new Random();
        random.nextBytes(smallContent);
        random.nextBytes(bigContent);

        MockSessionFactory factory = new MockSessionFactory();
        final HTTP2ServerDecoder decoder = new HTTP2ServerDecoder();
        final com.firefly.net.Session session = factory.create();
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");

        final Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

        final HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(http2Configuration, session, null,
                new ServerSessionListener() {

                    @Override
                    public Map<Integer, Integer> onPreface(Session session) {
                        System.out.println("on preface: " + session.isClosed());
                        Assert.assertThat(session.isClosed(), is(false));
                        return settings;
                    }

                    @Override
                    public Listener onNewStream(Stream stream, HeadersFrame frame) {
                        System.out.println("on new stream: " + stream.getId());
                        System.out.println("on new stread headers: " + frame.getMetaData().toString());

                        Assert.assertThat(stream.getId(), is(5));
                        Assert.assertThat(frame.getMetaData().getHttpVersion(), is(HttpVersion.HTTP_2));
                        Assert.assertThat(frame.getMetaData().getFields().get("User-Agent"), is("Firefly Client 1.0"));
                        Assert.assertThat(frame.getMetaData().getFields().get(HttpHeader.CONTENT_LENGTH), is("72"));

                        MetaData.Request request = (MetaData.Request) frame.getMetaData();
                        Assert.assertThat(request.getMethod(), is("POST"));
                        Assert.assertThat(request.getURI().getPath(), is("/data"));
                        Assert.assertThat(request.getURI().getPort(), is(8080));
                        Assert.assertThat(request.getURI().getHost(), is("localhost"));
                        return new Listener() {

                            @Override
                            public void onHeaders(Stream stream, HeadersFrame frame) {
                                System.out.println("on headers: " + frame.getMetaData());
                            }

                            @Override
                            public Listener onPush(Stream stream, PushPromiseFrame frame) {
                                return null;
                            }

                            @Override
                            public void onData(Stream stream, DataFrame frame, Callback callback) {
                                Assert.assertThat(stream.getId(), is(5));
                                if (frame.isEndStream()) {
                                    Assert.assertThat(frame.remaining(), is(50));
                                    Assert.assertThat(frame.getData().array(), is(bigContent));
                                } else {
                                    Assert.assertThat(frame.remaining(), is(22));
                                    Assert.assertThat(frame.getData().array(), is(smallContent));
                                }
                                System.out.println("data size:" + frame.remaining());
                                callback.succeeded();
                            }

                            @Override
                            public void onReset(Stream stream, ResetFrame frame) {

                            }

                            @Override
                            public boolean onIdleTimeout(Stream stream, Throwable x) {
                                return true;
                            }
                        };
                    }

                    @Override
                    public void onSettings(Session session, SettingsFrame frame) {
                        System.out.println("on settings: " + frame.toString());
                        Assert.assertThat(frame.getSettings().get(SettingsFrame.INITIAL_WINDOW_SIZE), is(http2Configuration.getInitialStreamSendWindow()));
                    }

                    @Override
                    public void onPing(Session session, PingFrame frame) {
                    }

                    @Override
                    public void onReset(Session session, ResetFrame frame) {
                    }

                    @Override
                    public void onClose(Session session, GoAwayFrame frame) {
                    }

                    @Override
                    public void onFailure(Session session, Throwable failure) {
                    }

                    @Override
                    public void onAccept(Session session) {
                    }

                    @Override
                    public boolean onIdleTimeout(Session session) {
                        // TODO Auto-generated method stub
                        return false;
                    }
                });
        session.attachObject(http2ServerConnection);

        int streamId = 5;
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.ACCEPT, "text/html");
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        fields.put(HttpHeader.CONTENT_LENGTH, "72");
        MetaData.Request metaData = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField("localhost:8080"), "/data", HttpVersion.HTTP_2, fields);

        DataFrame smallDataFrame = new DataFrame(streamId, ByteBuffer.wrap(smallContent), false);
        DataFrame bigDateFrame = new DataFrame(streamId, ByteBuffer.wrap(bigContent), true);

        Generator generator = new Generator(http2Configuration.getMaxDynamicTableSize(), http2Configuration.getMaxHeaderBlockFragment());

        HeadersGenerator headersGenerator = generator.getControlGenerator(FrameType.HEADERS);
        SettingsGenerator settingsGenerator = generator.getControlGenerator(FrameType.SETTINGS);

        List<ByteBuffer> list = new LinkedList<>();
        list.add(ByteBuffer.wrap(PrefaceFrame.PREFACE_BYTES));
        list.add(settingsGenerator.generateSettings(settings, false));
        list.addAll(headersGenerator.generateHeaders(streamId, metaData, null, false));
        list.addAll(generator.data(smallDataFrame, smallContent.length).second);
        list.addAll(generator.data(bigDateFrame, bigContent.length).second);

        for (ByteBuffer buffer : list) {
            decoder.decode(buffer, session);
        }
        System.out.println("out data: " + factory.output.size());
        Assert.assertThat(factory.output.size(), greaterThan(1));
        http2ServerConnection.close();
    }

    @Test
    public void testHeaders() throws Throwable {
        MockSessionFactory factory = new MockSessionFactory();
        final HTTP2ServerDecoder decoder = new HTTP2ServerDecoder();
        final com.firefly.net.Session session = factory.create();
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        final HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(http2Configuration, session, null,
                new ServerSessionListener() {

                    @Override
                    public Map<Integer, Integer> onPreface(Session session) {
                        System.out.println("on preface: " + session.isClosed());
                        Assert.assertThat(session.isClosed(), is(false));
                        return null;
                    }

                    @Override
                    public Listener onNewStream(Stream stream, HeadersFrame frame) {
                        System.out.println("on new stream: " + stream.getId());
                        System.out.println("on new stream headers: " + frame.getMetaData().toString());

                        Assert.assertThat(stream.getId(), is(5));
                        Assert.assertThat(frame.getMetaData().getHttpVersion(), is(HttpVersion.HTTP_2));
                        Assert.assertThat(frame.getMetaData().getFields().get("User-Agent"), is("Firefly Client 1.0"));

                        MetaData.Request request = (MetaData.Request) frame.getMetaData();
                        Assert.assertThat(request.getMethod(), is("GET"));
                        Assert.assertThat(request.getURI().getPath(), is("/index"));
                        Assert.assertThat(request.getURI().getPort(), is(8080));
                        Assert.assertThat(request.getURI().getHost(), is("localhost"));
                        return new Listener() {

                            @Override
                            public void onHeaders(Stream stream, HeadersFrame frame) {
                                System.out.println("on headers: " + frame.getMetaData());
                            }

                            @Override
                            public Listener onPush(Stream stream, PushPromiseFrame frame) {
                                return null;
                            }

                            @Override
                            public void onData(Stream stream, DataFrame frame, Callback callback) {

                            }

                            @Override
                            public void onReset(Stream stream, ResetFrame frame) {

                            }

                            @Override
                            public boolean onIdleTimeout(Stream stream, Throwable x) {
                                return true;
                            }
                        };
                    }

                    @Override
                    public void onSettings(Session session, SettingsFrame frame) {
                        System.out.println("on settings: " + frame.toString());
                        Assert.assertThat(frame.getSettings().get(SettingsFrame.INITIAL_WINDOW_SIZE), is(http2Configuration.getInitialStreamSendWindow()));
                    }

                    @Override
                    public void onPing(Session session, PingFrame frame) {
                    }

                    @Override
                    public void onReset(Session session, ResetFrame frame) {
                    }

                    @Override
                    public void onClose(Session session, GoAwayFrame frame) {
                    }

                    @Override
                    public void onFailure(Session session, Throwable failure) {
                    }

                    @Override
                    public void onAccept(Session session) {
                    }

                    @Override
                    public boolean onIdleTimeout(Session session) {
                        // TODO Auto-generated method stub
                        return false;
                    }
                });
        session.attachObject(http2ServerConnection);

        int streamId = 5;
        HttpFields fields = new HttpFields();
        fields.put("Accept", "text/html");
        fields.put("User-Agent", "Firefly Client 1.0");
        MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField("localhost:8080"), "/index", HttpVersion.HTTP_2, fields);
        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

        Generator generator = new Generator(http2Configuration.getMaxDynamicTableSize(), http2Configuration.getMaxHeaderBlockFragment());

        HeadersGenerator headersGenerator = generator.getControlGenerator(FrameType.HEADERS);
        SettingsGenerator settingsGenerator = generator.getControlGenerator(FrameType.SETTINGS);

        List<ByteBuffer> list = new LinkedList<>();
        list.add(ByteBuffer.wrap(PrefaceFrame.PREFACE_BYTES));
        list.add(settingsGenerator.generateSettings(settings, false));
        list.addAll(headersGenerator.generateHeaders(streamId, metaData, null, true));
        for (ByteBuffer buffer : list) {
            decoder.decode(buffer, session);
        }

        Assert.assertThat(factory.output.size(), greaterThan(0));
        System.out.println("out data: " + factory.output.size());
        Assert.assertThat(factory.output.size(), greaterThan(1));
        http2ServerConnection.close();
    }
}
