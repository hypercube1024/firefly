package test.codec.http2;

import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HTTP2ServerTLSDemo {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public static void main(String[] args) {
        // System.setProperty("javax.net.debug", "all");

        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setSecureConnectionEnabled(true);
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

        final Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());

        HTTP2Server server = new HTTP2Server("localhost", 6677, http2Configuration, new ServerHTTPHandler.Adapter() {
            @Override
            public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
                                   HTTPConnection connection) {
                log.info("received data, {}", BufferUtils.toString(item, StandardCharsets.UTF_8));
                return false;
            }

            @Override
            public boolean messageComplete(Request request, Response response, HTTPOutputStream outputStream,
                                           HTTPConnection connection) {
                log.info("received end frame, {}, {}", request.getURI(), request.getFields());
                HttpURI uri = request.getURI();
                if (uri.getPath().equals("/index")) {
                    response.setStatus(200);
                    try (HTTPOutputStream output = outputStream) {
                        output.writeWithContentLength(
                                BufferUtils.toBuffer("receive initial stream successful", StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (uri.getPath().equals("/data")) {
                    response.setStatus(200);
                    try (HTTPOutputStream output = outputStream) {
                        output.write(
                                BufferUtils.toBuffer("receive data stream successful\r\n", StandardCharsets.UTF_8));
                        output.write(BufferUtils.toBuffer("thank you\r\n", StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (uri.getPath().equals("/data2")) {
                    response.setStatus(200);
                    try (HTTPOutputStream output = outputStream) {
                        ByteBuffer[] data = new ByteBuffer[]{
                                BufferUtils.toBuffer("receive data 2 stream successful\r\n", StandardCharsets.UTF_8),
                                BufferUtils.toBuffer("thank you 2 \r\n", StandardCharsets.UTF_8)};
                        output.writeWithContentLength(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response.setStatus(404);
                    try (HTTPOutputStream output = outputStream) {
                        output.writeWithContentLength(
                                BufferUtils.toBuffer(uri.getPath() + " not found", StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
        server.start();
    }

}
