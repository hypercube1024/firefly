package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;

/**
 * @author Pengtao Qiu
 */
public class RoutingWithRegexDemo {
    public static void main(String[] args) {
        $.httpServer().router()
         .method(HttpMethod.GET).pathRegex("/hello(\\d*)")
         .handler(ctx -> {
             String group1 = ctx.getRouterParameter("group1");
             ctx.write("match path: " + ctx.getURI().getPath()).write("\r\n")
                .end("capture group1: " + group1);
         }).listen("localhost", 8080);
    }
}
