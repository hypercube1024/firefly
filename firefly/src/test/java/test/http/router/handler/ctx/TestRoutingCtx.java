package test.http.router.handler.ctx;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.RoutingContext;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.Optional;
import java.util.concurrent.Phaser;

import static com.firefly.server.http2.HTTP2ServerBuilder.getCurrentCtx;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestRoutingCtx extends AbstractHTTPHandlerTest {

    @Test
    public void test() {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder s = $.httpServer();
        s.router().get("/testCtx").asyncHandler(ctx -> {
            ctx.setAttribute("hiCtx", "Woo");
            ctx.next();
        })
         .router().get("/testCtx").asyncHandler(ctx -> testCtx())
         .listen(host, port);

        $.httpClient().get(uri + "/testCtx").submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("Woo"));
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        s.stop();
        $.httpClient().stop();
    }

    private void testCtx() {
        Optional<RoutingContext> ctx = getCurrentCtx();
        Assert.assertThat(ctx.isPresent(), is(true));
        ctx.ifPresent(c -> c.end(ctx.map(RoutingContext::getAttributes)
                                    .map(m -> (String) m.get("hiCtx"))
                                    .orElse("empty")));
    }
}
