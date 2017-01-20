package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.io.BufferUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientDemo5 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        System.setProperty("debugMode", "true");
        HTTP2Configuration httpConfiguration = new HTTP2Configuration();
        httpConfiguration.setSecureConnectionEnabled(true);

        SimpleHTTPClient client = new SimpleHTTPClient(httpConfiguration);

        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                client.get("https://login.taobao.com/")
                      .submit()
                      .thenApply(res -> res.getStringBody("GBK"))
                      .thenAccept(System.out::println)
                      .thenAccept(v -> {
                          System.out.print("------------------------");
                          System.out.println("time: " + (System.currentTimeMillis() - start));
                      });
            }
            Thread.sleep(5000L);
        }
    }

    public static void main3(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        for (int i = 0; i < 200; i++) {
            try {
                long start = System.currentTimeMillis();
                CompletableFuture<SimpleResponse> future = client.get("http://www.csdn.net").submit();
                SimpleResponse response = future.get(2, TimeUnit.SECONDS);
                long end = System.currentTimeMillis();
                System.out.println(response.getResponse());
                System.out.println(response.getResponse().getFields());
                System.out.println(response.getResponse().getContentLength() + "|" + (end - start));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main2(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        client.get("http://www.csdn.net")
              .headerComplete(res -> {
                  System.out.println(res.toString());
                  System.out.println(res.getFields());
              })
              .content(buf -> System.out.println(BufferUtils.toUTF8String(buf)))
              .messageComplete(res -> System.out.println("ok"))
              .end();

    }
}
