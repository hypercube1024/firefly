package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class RoutingByPathsWithWildcardDemo {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/product*")
         .handler(ctx -> ctx.end("current path is " + ctx.getURI().getPath()))
         .router().get("/*items*")
         .handler(ctx -> ctx.end("current path is " + ctx.getURI().getPath()))
         .listen("localhost", 8080);
    }
}
