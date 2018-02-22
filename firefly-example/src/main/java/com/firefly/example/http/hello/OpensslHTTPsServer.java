package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class OpensslHTTPsServer {
    public static void main(String[] args) {
        $.httpsServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
