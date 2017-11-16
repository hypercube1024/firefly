package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class CapturingPathParameterDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer().router().get("/product/:id")
         .handler(ctx -> {
             String id = ctx.getRouterParameter("id");
             ctx.end($.string.replace("Get the product {}", id));
         }).listen(host, port);

        $.httpClient()
         .get($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
