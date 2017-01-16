package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author Pengtao Qiu
 */
public class ProxyDemo {

    public static void main(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
//        configuration.setSecureConnectionEnabled(true);
        SimpleHTTPServer server = new SimpleHTTPServer(configuration);
        server.headerComplete(request -> request.messageComplete(req -> {
            SimpleResponse response = req.getResponse();

            System.out.println(req.getRequest().toString());
            System.out.println(req.getRequest().getFields());
            long length = req.getRequest().getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());

            long start = System.currentTimeMillis();
            try {
                SimpleHTTPClient.RequestBuilder builder = client.request(req.getRequest().getMethod(), req.getRequest().getURI().toURI().toURL())
                                                                .addAll(req.getRequest().getFields());

                if (length > 0L) {
                    builder.output(output -> {
                        try (HTTPOutputStream out = output) {
                            out.writeWithContentLength(req.getRequestBody().toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                builder.submit()
                       .thenAccept(dst -> {
                           response.getResponse().setStatus(dst.getResponse().getStatus());
                           response.getResponse().setReason(dst.getResponse().getReason());
                           response.getResponse().setHttpVersion(dst.getResponse().getHttpVersion());
                           response.getResponse()
                                   .getFields()
                                   .addAll(dst.getResponse().getFields());
                           try (OutputStream out = response.getOutputStream()) {
                               dst.getResponseBody().forEach(buffer -> {
                                   try {
                                       out.write(BufferUtils.toArray(buffer));
                                   } catch (IOException e) {
                                       e.printStackTrace();
                                   }
                               });
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                           System.out.println("time: " + (System.currentTimeMillis() - start));
                       })
                       .exceptionally(e -> {
                           e.printStackTrace();
                           return null;
                       });
            } catch (MalformedURLException | URISyntaxException e) {
                e.printStackTrace();
            }
            System.out.println("block time: " + (System.currentTimeMillis() - start) + "|" + request.getRequest().getURIString());
        })).listen("localhost", 3344);
    }
}
