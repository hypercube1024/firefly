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
            for (int i = 0; i < 1; i++) {
                long start = System.currentTimeMillis();
                client.get("https://www.baidu.com").submit().thenAccept(res -> {
                    String body = res.getStringBody("UTF-8");
                    System.out.println(body);
                    long end = System.currentTimeMillis();
                    System.out.println("time: " + (end - start));
                });
            }
            Thread.sleep(5000L);
        }

        client.stop();
    }
}
