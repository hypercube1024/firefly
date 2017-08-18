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
        httpServer.router().get("/routerChain").handler(ctx -> {
            ctx.setAttribute("reqId", 1000);
            ctx.write("enter router 1\r\n").next(new Promise<String>() {
                @Override
                public void succeeded(String result) {
                    ctx.write("router 1 success\r\n").end(result);
                }

                @Override
                public void failed(Throwable x) {
                    ctx.end(x.getMessage());
                }
            });
        }).router().get("/routerChain").handler(ctx -> {
            Integer reqId = (Integer) ctx.getAttribute("reqId");
            ctx.write("enter router 2, request id " + reqId + "\r\n").next(new Promise<String>() {
                @Override
                public void succeeded(String result) {
                    ctx.write("router 2 success, request id " + reqId + "\r\n");
                }
            });
        }).router().get("/routerChain").handler(ctx -> {
            Integer reqId = (Integer) ctx.getAttribute("reqId");
            ctx.write("enter router 3, request id " + reqId + "\r\n").complete(new Promise<String>() {
                @Override
                public void succeeded(String result) {
                    ctx.write("router 3 success, request id " + reqId + "\r\n");
                }
            }).succeed("request complete");
        }).listen(host, port);

        SimpleResponse response = $.httpClient().get(uri + "/routerChain").submit().get(2, TimeUnit.SECONDS);
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
