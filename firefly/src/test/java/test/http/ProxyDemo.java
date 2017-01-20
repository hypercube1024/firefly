package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Pengtao Qiu
 */
public class ProxyDemo {

    public static void main(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        SimpleHTTPServer server = new SimpleHTTPServer();

        server.headerComplete(srcRequest -> {
            long start = System.currentTimeMillis();
            System.out.println(srcRequest.getRequest().toString());
            System.out.println(srcRequest.getRequest().getFields());
            try {
                // copy origin request line and headers to destination request
                Promise.Completable<HTTPOutputStream> outputCompletable = new Promise.Completable<>();
                SimpleHTTPClient.RequestBuilder dstReq = client.request(srcRequest.getRequest().getMethod(), srcRequest.getRequest().getURI().toURI().toURL())
                                                               .addAll(srcRequest.getRequest().getFields())
                                                               .output(outputCompletable);

                long contentLength = srcRequest.getRequest().getFields().getLongField(HttpHeader.CONTENT_LENGTH.asString());
                if (contentLength > 0) {
                    // transmit origin request body to destination server
                    AtomicLong count = new AtomicLong();
                    srcRequest.content(srcBuffer -> outputCompletable.thenAccept(dstOutput -> {
                        try {
                            if (count.addAndGet(srcBuffer.remaining()) < contentLength) {
                                dstOutput.write(srcBuffer);
                            } else {
                                dstOutput.write(srcBuffer);
                                dstOutput.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
                } else {
                    outputCompletable.thenAccept(dstOutput -> {
                        try {
                            dstOutput.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                srcRequest.messageComplete(req -> {
                    SimpleResponse srcResponse = req.getAsyncResponse();
                    dstReq.headerComplete(dstResponse -> {
                        // copy destination server response line and headers to origin response
                        System.out.println(dstResponse.toString());
                        System.out.println(dstResponse.getFields());
                        srcResponse.getResponse().setStatus(dstResponse.getStatus());
                        srcResponse.getResponse().setReason(dstResponse.getReason());
                        srcResponse.getResponse().setHttpVersion(dstResponse.getHttpVersion());
                        srcResponse.getResponse().getFields().addAll(dstResponse.getFields());
                    }).content(dstBuffer -> {
                        // transmit destination server response body
                        System.out.println("receive dst data -> " + dstBuffer.remaining());
                        try {
                            srcResponse.getOutputStream().write(BufferUtils.toArray(dstBuffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).messageComplete(dstResponse -> {
                        try {
                            srcResponse.getOutputStream().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("time: " + (System.currentTimeMillis() - start));
                    }).end();
                });
                System.out.println("block time: " + (System.currentTimeMillis() - start) + "|" + srcRequest.getRequest().getURIString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).listen("localhost", 3344);
    }
}
