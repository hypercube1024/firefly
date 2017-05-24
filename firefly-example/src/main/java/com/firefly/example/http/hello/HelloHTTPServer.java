package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class HelloHTTPServer {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }
}
