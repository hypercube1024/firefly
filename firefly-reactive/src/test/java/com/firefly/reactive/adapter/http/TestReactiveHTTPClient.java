package com.firefly.reactive.adapter.http;

import com.firefly.$;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.reactive.adapter.Reactor;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.concurrent.Promise;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestReactiveHTTPClient {

    String host = "localhost";
    int port = 8000;
    String uri;

    @Before
    public void init() {
        port = (int) RandomUtils.random(3000, 65534);
        uri = $.uri.newURIBuilder("http", host, port).toString();
        System.out.println(uri);
    }

    @Test
    public void test() {
        ReactiveHTTPClient c = Reactor.http.httpClient();
        HTTP2ServerBuilder s = $.httpServer();

        s.router().get("/*")
         .asyncHandler(ctx -> ctx.next(new Promise<String>() {
             @Override
             public void succeeded(String result) {
                 ctx.end();
             }

             @Override
             public void failed(Throwable x) {
                 ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .end(x.getMessage());
             }
         }))
         .router().get("/reactor/hello")
         .asyncHandler(ctx -> ctx.write("hello reactor").succeed(null))
         .listen(host, port);

        c.get(uri + "/reactor/hello").toMono()
         .map(SimpleResponse::getStringBody)
         .doOnSuccess(System.out::println)
         .doOnSuccess(body -> Assert.assertThat(body, is("hello reactor")))
         .block();

        s.stop();
        c.stop();
    }
}
