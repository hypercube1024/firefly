package test.codec.http2;

import com.firefly.client.http2.*;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.concurrent.FuturePromise;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static test.codec.http2.HTTPClientHandlerFactory.newHandler;

public class HTTP2ClientH2cDemo2 {

    public static void main(String[] args) throws Exception {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
        HTTP2Client client = new HTTP2Client(http2Configuration);

        FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
        client.connect("127.0.0.1", 6677, promise);

        final HTTPClientConnection httpConnection = promise.get();
        HTTPClientRequest request = new HTTPClientRequest("GET", "/index");

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);

        final ByteBuffer[] buffers = new ByteBuffer[]{
                ByteBuffer.wrap("hello world!".getBytes("UTF-8")),
                ByteBuffer.wrap("big hello world!".getBytes("UTF-8"))};
        FuturePromise<HTTP2ClientConnection> http2Promise = new FuturePromise<>();
        ClientHTTPHandler handler = newHandler(buffers);

        httpConnection.upgradeHTTP2(request, settingsFrame, http2Promise, handler);

        HTTPClientConnection clientConnection = http2Promise.get();

        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");
        MetaData.Request post = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField("127.0.0.1:6677"),
                "/data", HttpVersion.HTTP_1_1, fields);
        clientConnection.sendRequestWithContinuation(post, handler);

        MetaData.Request get = new MetaData.Request("GET", HttpScheme.HTTP,
                new HostPortHttpField("127.0.0.1:6677"),
                "/test2", HttpVersion.HTTP_1_1, new HttpFields());
        clientConnection.send(get, handler);

        MetaData.Request post2 = new MetaData.Request("POST", HttpScheme.HTTP,
                new HostPortHttpField("127.0.0.1:6677"),
                "/data", HttpVersion.HTTP_1_1, fields);
        clientConnection.send(post2, new ByteBuffer[]{
                ByteBuffer.wrap("test data 2".getBytes("UTF-8")),
                ByteBuffer.wrap("finished test data 2".getBytes("UTF-8"))}, handler);
    }

}
