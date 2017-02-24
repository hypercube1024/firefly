package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class CapturingPathParameterDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/good/:type/:id")
         .handler(ctx -> {
            String type = ctx.getRouterParameter("type");
            String id = ctx.getRouterParameter("id");
            ctx.end("get good type: " + type + ", id: " + id);
         }).listen("localhost", 8080);
    }
}
