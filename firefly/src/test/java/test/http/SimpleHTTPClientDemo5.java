package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;

import java.util.concurrent.ExecutionException;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientDemo5 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        System.setProperty("debugMode", "true");
        SimpleHTTPClientConfiguration httpConfiguration = new SimpleHTTPClientConfiguration();
        httpConfiguration.setSecureConnectionEnabled(true);

        SimpleHTTPClient client = new SimpleHTTPClient(httpConfiguration);

        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 5; i++) {
                long start = System.currentTimeMillis();
                client.get("https://www.taobao.com")
                      .submit()
                      .thenApply(res -> res.getStringBody("UTF-8"))
                      .thenAccept(System.out::println)
                      .thenAccept(v -> {
                          System.out.print("------------------------");
                          System.out.println("time: " + (System.currentTimeMillis() - start));
                      });
            }
            Thread.sleep(5000L);
        }
    }
}
