package com.firefly.example.reactive.coffee.store;

import com.firefly.$;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
public class AppMain {

    private static final String root = "template/coffeeStore";

    public static void main(String[] args) {
        HTTP2ServerBuilder s = $.httpServer();
        s.router().get("/").asyncHandler(ctx -> {
            ctx.renderTemplate(root + "/index.mustache", Collections.emptyList());
        });

        s.listen("localhost", 8080);
    }
}
