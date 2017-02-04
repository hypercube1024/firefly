package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.utils.concurrent.Promise;

import java.util.concurrent.ExecutionException;

public class SimpleHTTPClientDemo4 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SimpleHTTPClient client = new SimpleHTTPClient();

        for (int i = 0; i < 5; i++) {
            client.post("http://localhost:3333/postData")
                  .put("RequestId", i + "_")
                  .body("test post data, hello foo " + i)
                  .submit(r -> System.out.println(r.getStringBody()));
        }

        for (int i = 10; i < 20; i++) {
            client.post("http://localhost:3333/postData")
                  .put("RequestId", i + "_")
                  .body("test post data, hello foo " + i)
                  .submit()
                  .thenAcceptAsync(r -> System.out.println(r.getStringBody()));
        }

        for (int i = 20; i < 30; i++) {
            Promise.Completable<SimpleResponse> future = client
                    .post("http://localhost:3333/postData")
                    .put("RequestId", i + "_")
                    .body("test post data, hello foo " + i)
                    .submit();
            SimpleResponse r = future.get();
            System.out.println(r.getStringBody());
        }

        Thread.sleep(5000);
        client.stop();

    }

}
