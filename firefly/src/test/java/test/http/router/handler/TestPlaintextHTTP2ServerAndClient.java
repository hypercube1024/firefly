package test.http.router.handler;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.server.http2.HTTP2ServerBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPlaintextHTTP2ServerAndClient extends AbstractHTTPHandlerTest {

    @Test
    public void test() {
        int times = 5;
        Phaser phaser = new Phaser(times + 1);
        HTTP2ServerBuilder server = $.plaintextHTTP2Server();
        server.router().post("/plaintextHttp2").handler(ctx -> {
            System.out.println(ctx.getHttpVersion().asString() + "\r\n" +
                    ctx.getFields() +
                    ctx.getStringBody() +
                    "\r\n-----------------------\r\n");
            ctx.end("test plaintext http2");
        }).listen(host, port);

        SimpleHTTPClient client = $.createPlaintextHTTP2Client();
        for (int i = 0; i < times; i++) {
            client.post(uri + "/plaintextHttp2").body("post data").submit()
                  .thenAccept(res -> {
                      System.out.println(res.getStatus() + " " + res.getHttpVersion().asString() + "\r\n" +
                              res.getFields() +
                              res.getStringBody() +
                              "\r\n-----------------------\r\n");
                      Assert.assertThat(res.getStringBody(), is("test plaintext http2"));
                      phaser.arrive();
                  });
        }

        phaser.arriveAndAwaitAdvance();
        server.stop();
        client.stop();
    }
}
