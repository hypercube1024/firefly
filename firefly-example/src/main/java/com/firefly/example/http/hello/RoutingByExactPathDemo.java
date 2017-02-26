package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class RoutingByExactPathDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/product/tools")
         .handler(ctx -> ctx.write("spanner: 3").write("\r\n")
                            .write("pliers: 1").write("\r\n")
                            .end("screwdriver: 1"))
         .listen("localhost", 8080);
    }
}
