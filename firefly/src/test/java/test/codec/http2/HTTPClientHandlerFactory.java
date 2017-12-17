package test.codec.http2;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class HTTPClientHandlerFactory {

    public static ClientHTTPHandler.Adapter newHandler(ByteBuffer[] buffers) {
        return new ClientHTTPHandler.Adapter() {
            @Override
            public void continueToSendData(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("client received 100 continue");
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
                return false;
            }

            @Override
            public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
                System.out.println("client received frame: " + response.getStatus() + ", " + response.getReason());
                System.out.println(response.getFields());
                System.out.println("---------------------------------");
                return true;
            }
        };
    }
}
