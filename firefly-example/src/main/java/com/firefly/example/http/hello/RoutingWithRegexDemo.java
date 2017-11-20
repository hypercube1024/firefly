package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;

/**
 * @author Pengtao Qiu
 */
public class RoutingWithRegexDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().method(HttpMethod.GET).pathRegex("/product(.*)")
         .handler(ctx -> {
             String matched = ctx.getRegexGroup(1);
             ctx.write("Intercept the product: " + matched + "\r\n").next();
         })
         .router().get("/product/:type")
         .handler(ctx -> {
             String type = ctx.getPathParameter("type");
             ctx.end("List " + type + "\r\n");
         })
         .listen(host, port);

        $.httpClient().get($.string.replace("http://{}:{}/product/orange", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
