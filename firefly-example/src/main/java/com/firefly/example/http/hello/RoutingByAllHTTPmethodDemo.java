package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class RoutingByAllHTTPmethodDemo {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().path("/all-methods")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .listen("localhost", 8080);

        $.httpClient().post("http://localhost:8080/all-methods").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().put("http://localhost:8080/all-methods").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
