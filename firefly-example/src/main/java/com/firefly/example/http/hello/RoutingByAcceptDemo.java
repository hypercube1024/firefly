package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class RoutingByAcceptDemo {

    public static class Apple {
        public String color;
        public double weight;

        @Override
        public String toString() {
            return "color[" + color + "], weight[" + weight + "]";
        }
    }

    public static void main(String[] args) {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().get("/apple/:id").produces("application/json")
              .handler(ctx -> {
                  Apple apple = new Apple();
                  apple.weight = 1.2;
                  apple.color = "red";
                  ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.asString())
                     .end($.json.toJson(apple));
              }).listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/apple/1")
         .put(HttpHeader.ACCEPT, "text/plain; q=0.9, application/json").submit()
         .thenAccept(res -> {
             System.out.println(res.getJsonBody(Apple.class));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
