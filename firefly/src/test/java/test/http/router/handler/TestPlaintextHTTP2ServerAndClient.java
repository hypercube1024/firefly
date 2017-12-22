package test.http.router.handler;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.server.http2.HTTP2ServerBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPlaintextHTTP2ServerAndClient extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws InterruptedException {
        int times = 10;
        CountDownLatch latch = new CountDownLatch(times);

        HTTP2ServerBuilder server = $.plaintextHTTP2Server();
        server.router().post("/plaintextHttp2").handler(ctx -> {
            System.out.println("Server: " +
                    ctx.getHttpVersion().asString() + "\r\n" +
                    ctx.getFields() +
                    ctx.getStringBody() +
                    "\r\n-----------------------\r\n");
            ctx.end("test plaintext http2");
        }).listen(host, port);

        SimpleHTTPClientConfiguration config = new SimpleHTTPClientConfiguration();
        config.setProtocol(HttpVersion.HTTP_2.asString());
        config.setPoolSize(1);
        SimpleHTTPClient client = new SimpleHTTPClient(config);
        for (int i = 0; i < times; i++) {
            client.post(uri + "/plaintextHttp2").body("post data").submit()
                  .thenAccept(res -> {
                      System.out.println("Client: " +
                              res.getStatus() + " " + res.getHttpVersion().asString() + "\r\n" +
                              res.getFields() +
                              res.getStringBody() +
                              "\r\n-----------------------\r\n");
                      Assert.assertThat(res.getStringBody(), is("test plaintext http2"));
                      latch.countDown();
                      System.out.println("Remain task: " + latch.getCount());
                  });
        }

        latch.await();
        System.out.println("All tasks complete");

        server.stop();
        client.stop();
    }
}
