package test.codec.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.ServerHTTPHandler;
import com.firefly.server.http2.ServerSessionListener;
import com.firefly.server.http2.WebSocketHandler;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.io.BufferUtils;

public class HTTP1ServerDemo3 {

    public static void main(String[] args) {
        final HTTP2Configuration http2Configuration = new HTTP2Configuration();
        http2Configuration.getTcpConfiguration().setTimeout(10 * 60 * 1000);

        HTTP2Server server = new HTTP2Server("localhost", 6678, http2Configuration, new ServerSessionListener.Adapter(),
                new ServerHTTPHandler.Adapter() {

                    @Override
                    public void earlyEOF(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                                         HTTPConnection connection) {
                        System.out.println("the server connection " + connection.getSessionId() + " is early EOF");
                    }

                    @Override
                    public void badMessage(int status, String reason, MetaData.Request request,
                                           MetaData.Response response, HTTPOutputStream output, HTTPConnection connection) {
                        System.out.println("the server received a bad message, " + status + "|" + reason);

                        try {
                            connection.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output, HTTPConnection connection) {
                        System.out
                                .println("server received data: " + BufferUtils.toString(item, StandardCharsets.UTF_8));
                        return false;
                    }

                    @Override
                    public boolean accept100Continue(MetaData.Request request, MetaData.Response response,
                                                     HTTPOutputStream output, HTTPConnection connection) {
                        System.out.println(
                                "the server received a 100 continue header, the path is " + request.getURI().getPath());
                        return false;
                    }

                    @Override
                    public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                                   HTTPOutputStream outputStream, HTTPConnection connection) {
                        HttpURI uri = request.getURI();
                        System.out.println("current path is " + uri.getPath());
                        System.out.println("current parameter string is " + uri.getQuery());
                        System.out.println("current http headers are " + request.getFields());
                        MultiMap<String> parameterMap = new MultiMap<String>();
                        uri.decodeQueryTo(parameterMap);
                        System.out.println("current parameters are " + parameterMap);

                        if (uri.getPath().equals("/index")) {
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
                                output.writeWithContentLength(BufferUtils.toBuffer("receive Continue-100 successfully ",
                                        StandardCharsets.UTF_8));
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
                    }

                }, new WebSocketHandler() {
        });
        server.start();
    }

}
