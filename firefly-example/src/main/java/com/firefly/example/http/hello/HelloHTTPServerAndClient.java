package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class HelloHTTPServerAndClient {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/").handler(ctx -> ctx.write("hello world! ").next())
                  .router().get("/").handler(ctx -> ctx.end("end message"))
                  .listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        $.httpClient().get("http://localhost:8080/hello").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}
