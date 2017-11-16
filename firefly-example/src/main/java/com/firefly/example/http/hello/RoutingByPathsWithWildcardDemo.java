package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class RoutingByPathsWithWildcardDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().get("/product*")
         .handler(ctx -> {
             String matched = ctx.getWildcardMatchedResult(0);
             ctx.write("Intercept the product: " + matched + "\r\n").next();
         })
         .router().get("/product/:type")
         .handler(ctx -> {
             String type = ctx.getPathParameter("type");
             ctx.end("List " + type + "\r\n");
         })
         .listen(host, port);

        $.httpClient().get($.string.replace("http://{}:{}/product/apple", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
