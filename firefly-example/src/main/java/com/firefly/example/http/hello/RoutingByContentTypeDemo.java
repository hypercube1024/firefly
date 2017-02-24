package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.concurrent.Promise.Completable;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class RoutingByContentTypeDemo {

    public static class Product {
        public int id;
        public String name;

        @Override
        public String toString() {
            return "id[" + id + "], name[" + name + "]";
        }
    }

    public static void main(String[] args) throws Exception {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().put("/product/:id").consumes("application/json")
              .handler(ctx -> {
                  Product product = ctx.getJsonBody(Product.class);
                  ctx.end("update product: " + product + " success");
              })
              .router().post("/product").consumes("*/json")
              .handler(ctx -> {
                  Product product = ctx.getJsonBody(Product.class);
                  ctx.write("content type: " + ctx.getRouterParameter("param0"))
                     .write("\r\n")
                     .end("create product: " + product + " success");
              }).listen("localhost", 8080);

        Product product = new Product();
        product.name = "new book";
        Completable<SimpleResponse> c = $.httpClient().post("http://localhost:8080/product")
                                         .jsonBody(product)
                                         .submit();
        System.out.println(c.get().getStringBody());

        product = new Product();
        product.id = 1;
        product.name = "old book";
        $.httpClient().put("http://localhost:8080/product/1").jsonBody(product).submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
