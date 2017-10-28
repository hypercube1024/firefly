package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.utils.concurrent.Promise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;

public class SimpleHTTPClientDemo4 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SimpleHTTPClient client = new SimpleHTTPClient();

        Phaser phaser = new Phaser(10 + 20 + 1);
        for (int i = 0; i < 10; i++) {
            client.post("http://localhost:3333/postData")
                  .put("RequestId", i + "_")
                  .body("test post data, hello foo " + i)
                  .submit(r -> {
                      System.out.println(r.getStringBody());
                      phaser.arrive();
                  });
        }

        for (int i = 10; i < 30; i++) {
            client.post("http://localhost:3333/postData")
                  .put("RequestId", i + "_")
                  .body("test post data, hello foo " + i)
                  .submit()
                  .thenAcceptAsync(r -> {
                      System.out.println(r.getStringBody());
                      phaser.arrive();
                  });
        }

        for (int i = 30; i < 40; i++) {
            CompletableFuture<SimpleResponse> future = client
                    .post("http://localhost:3333/postData")
                    .put("RequestId", i + "_")
                    .body("test post data, hello foo " + i)
                    .submit();
            SimpleResponse r = future.get();
            System.out.println(r.getStringBody());
        }

        phaser.arriveAndAwaitAdvance();
        client.stop();
    }

}
