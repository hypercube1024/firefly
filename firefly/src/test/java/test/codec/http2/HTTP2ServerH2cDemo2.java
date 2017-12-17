package test.codec.http2;

import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.firefly.utils.io.BufferUtils.toBuffer;

public class HTTP2ServerH2cDemo2 {

    public static void main(String[] args) {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.setFlowControlStrategy("simple");
        http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);

        HTTP2Server server = new HTTP2Server("127.0.0.1", 6677, http2Configuration, new ServerHTTPHandler.Adapter() {

            @Override
            public boolean accept100Continue(Request request, Response response, HTTPOutputStream output,
                                             HTTPConnection connection) {
                System.out.println("received expect continue ");
                return false;
            }

            @Override
            public boolean content(ByteBuffer item, Request request, Response response, HTTPOutputStream output,
                                   HTTPConnection connection) {
                System.out.println("received data: " + BufferUtils.toString(item, StandardCharsets.UTF_8));
                return false;
            }

            @Override
            public boolean messageComplete(Request request, Response response, HTTPOutputStream outputStream,
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
                            output.write(toBuffer("receive data stream successful", StandardCharsets.UTF_8));
                            output.write(toBuffer("thank you", StandardCharsets.UTF_8));
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
    }

}
