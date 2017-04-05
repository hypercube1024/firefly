package com.firefly.example.template.spi;

import com.firefly.$;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pengtao Qiu
 */
public class CustomErrorResponseDemo {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(CustomErrorResponseDemo.class.getResource("/").toURI());
        $.httpServer().router().get("/static/*")
         .handler(new StaticFileHandler(path.toAbsolutePath().toString()))
         .listen("localhost", 8080);
    }
}
