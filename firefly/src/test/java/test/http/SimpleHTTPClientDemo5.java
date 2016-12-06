package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientDemo5 {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        SimpleHTTPClient client = new SimpleHTTPClient();

        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 10; i++) {
                long start = System.currentTimeMillis();
                client.get("http://www.baidu.com").submit().thenAccept(res -> {
                    String body = res.getStringBody("GBK");
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
