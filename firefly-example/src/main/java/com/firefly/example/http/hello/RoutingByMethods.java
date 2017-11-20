package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class RoutingByMethods {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().get("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             ctx.end($.string.replace("Get the product {}", id));
         })
         .router().post("/product")
         .handler(ctx -> {
             String product = ctx.getStringBody();
             ctx.end($.string.replace("Create a new product: {}", product));
         })
         .router().put("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String product = ctx.getStringBody();
             ctx.end($.string.replace("Update the product {}: {}", id, product));
         })
         .router().delete("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             ctx.end($.string.replace("Delete the product {}", id));
         })
         .listen(host, port);

        $.httpClient()
         .get($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .post($.string.replace("http://{}:{}/product", host, port))
         .body("Car 20. The color is red.")
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .put($.string.replace("http://{}:{}/product/20", host, port))
         .body("Change the color from red to black.")
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .delete($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
