package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.io.BufferUtils;

public class HTTPServerDemo4 {

    public static void main(String[] args) {
        int length = 2500;
        StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            s.append('t');
        }
        final String data = s.toString();

        HTTP2Configuration http2Configuration = new HTTP2Configuration();
        HTTP2Server server = new HTTP2Server("localhost", 7777, http2Configuration,
                new ServerHTTPHandler.Adapter().messageComplete((request, response, outputStream, connection) -> {

                    HttpURI uri = request.getURI();
                    // System.out.println("current path is " + uri.getPath());
                    // System.out.println("current parameter string is " +
                    // uri.getQuery());
                    // System.out.println("current http headers are " +
                    // request.getFields());
                    MultiMap<String> parameterMap = new MultiMap<String>();
                    uri.decodeQueryTo(parameterMap);
                    // System.out.println("current parameters are " +
                    // parameterMap);

                    if (uri.getPath().equals("/test")) {
                        response.setStatus(200);
                        response.setHttpVersion(request.getHttpVersion());
                        response.getFields().add(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE);
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(BufferUtils.toBuffer(data, StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (uri.getPath().equals("/index")) {
                        response.setStatus(200);

                        List<ByteBuffer> list = new ArrayList<>();
                        list.add(BufferUtils.toBuffer("hello the server demo ", StandardCharsets.UTF_8));
                        list.add(BufferUtils.toBuffer("test chunk 1 ", StandardCharsets.UTF_8));
                        list.add(BufferUtils.toBuffer("test chunk 2 ", StandardCharsets.UTF_8));
                        list.add(BufferUtils.toBuffer("中文的内容，哈哈 ", StandardCharsets.UTF_8));
                        list.add(BufferUtils.toBuffer("靠！！！ ", StandardCharsets.UTF_8));

                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(list.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (uri.getPath().equals("/testContinue")) {
                        response.setStatus(200);
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(
                                    BufferUtils.toBuffer("receive Continue-100 successfully ", StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        response.setStatus(404);
                        try (HTTPOutputStream output = outputStream) {
                            output.writeWithContentLength(BufferUtils.toBuffer("找不到页面", StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return true;
                }));
        server.start();

    }

}
