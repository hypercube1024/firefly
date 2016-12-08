package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.ssl.DefaultSSLContextFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientDemo5 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        SimpleHTTPClientConfiguration httpConfiguration = new SimpleHTTPClientConfiguration();
        SSLContextFactory sslContextFactory = new DefaultSSLContextFactory();
        httpConfiguration.setSslContextFactory(sslContextFactory);
        httpConfiguration.setSecureConnectionEnabled(true);

        SimpleHTTPClient client = new SimpleHTTPClient(httpConfiguration);

        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                client.get("https://www.baidu.com")
                      .submit()
                      .thenApply(res -> res.getStringBody("UTF-8"))
                      .thenAccept(System.out::println)
                      .thenAccept(v -> {
                          System.out.print("------------------------");
                          System.out.println("time: " + (System.currentTimeMillis() - start));
                      });
            }
            Thread.sleep(3000L);
        }
    }
}
