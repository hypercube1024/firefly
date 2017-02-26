package test.http.router.handler.template;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestTemplate extends AbstractHTTPHandlerTest {

    @Test
    public void test() {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/example").handler(ctx -> {
            ctx.put(HttpHeader.CONTENT_TYPE, "text/plain");
            ctx.renderTemplate("template/example.mustache", new Example());
        }).listen(host, port);

        $.httpClient().get(uri + "/example").submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getFields().get(HttpHeader.CONTENT_TYPE), is("text/plain"));
             Assert.assertThat(res.getStringBody().length(), greaterThan(0));
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

}
