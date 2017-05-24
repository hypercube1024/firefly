package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class RoutingBySpecifiedHTTPmethodDemo {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(4);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().method(HttpMethod.GET).path("/get-or-post")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .router().post("/get-or-post")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().post("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().put("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStatus() + " " + res.getReason());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
