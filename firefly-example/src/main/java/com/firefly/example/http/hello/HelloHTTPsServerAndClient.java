package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class HelloHTTPsServerAndClient {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpsServer();
        httpServer.router().get("/").handler(ctx -> ctx.write("hello world! ").next())
                  .router().get("/").handler(ctx -> ctx.end("end message"))
                  .listen("localhost", 8081);

        $.httpsClient().get("https://localhost:8081/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        $.httpsClient().get("https://localhost:8081/hello").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpsClient().stop();
    }
}
