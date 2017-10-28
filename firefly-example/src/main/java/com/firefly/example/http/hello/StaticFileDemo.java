package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pengtao Qiu
 */
public class StaticFileDemo {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(StaticFileDemo.class.getResource("/").toURI());
        $.httpsServer().router().get("/static/*")
         .handler(new StaticFileHandler(path.toAbsolutePath().toString()))
         .listen("localhost", 8080);
    }
}
