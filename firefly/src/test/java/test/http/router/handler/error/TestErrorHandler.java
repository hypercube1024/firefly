package test.http.router.handler.error;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestErrorHandler extends AbstractHTTPHandlerTest {

    @Test
    public void test() {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/").handler(ctx -> ctx.write("hello world! ").next())
                  .router().get("/").handler(ctx -> ctx.end("end message"))
                  .listen(host, port);

        $.httpClient().get(uri + "/").submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("hello world! end message"));
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().get(uri + "/hello").submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.NOT_FOUND_404));
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}
