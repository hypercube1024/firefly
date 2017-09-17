package test.http.router.handler.chain;

import com.firefly.$;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.concurrent.Promise;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestRouterChain extends AbstractHTTPHandlerTest {

    @Test
    public void testChain() throws Exception {
        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/routerChain").asyncHandler(ctx -> {
            ctx.setAttribute("reqId", 1000);
            ctx.write("enter router 1\r\n")
               .<String>nextFuture()
               .thenAccept(result -> ctx.write("router 1 success\r\n").end(result))
               .exceptionally(ex -> {
                   ctx.end(ex.getMessage());
                   return null;
               });
        }).router().get("/routerChain").asyncHandler(ctx -> {
            Integer reqId = (Integer) ctx.getAttribute("reqId");
            ctx.write("enter router 2, request id " + reqId + "\r\n")
               .<String>nextFuture()
               .thenAccept(result -> ctx.write("router 2 success, request id " + reqId + "\r\n").succeed(result))
               .exceptionally(ex -> {
                   ctx.fail(ex);
                   return null;
               });
        }).router().get("/routerChain").asyncHandler(ctx -> {
            Integer reqId = (Integer) ctx.getAttribute("reqId");
            ctx.write("enter router 3, request id " + reqId + "\r\n")
               .<String>complete()
               .thenAccept(result -> ctx.write("router 3 success, request id " + reqId + "\r\n").succeed(result))
               .exceptionally(ex -> {
                   ctx.fail(ex);
                   return null;
               });
            ctx.succeed("request complete");
        }).listen(host, port);

        SimpleResponse response = $.httpClient().get(uri + "/routerChain").submit().get(2, TimeUnit.SECONDS);
        System.out.println(response.getStringBody());
        Assert.assertThat(response.getStringBody(), is(
                "enter router 1\r\n" +
                        "enter router 2, request id 1000\r\n" +
                        "enter router 3, request id 1000\r\n" +
                        "router 3 success, request id 1000\r\n" +
                        "router 2 success, request id 1000\r\n" +
                        "router 1 success\r\n" +
                        "request complete"));
        httpServer.stop();
        $.httpClient().stop();
    }
}
