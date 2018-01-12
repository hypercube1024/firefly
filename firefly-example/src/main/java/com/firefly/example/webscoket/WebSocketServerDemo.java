package com.firefly.example.webscoket;

import com.firefly.$;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class WebSocketServerDemo {

    public static void main(String[] args) throws Exception {
        Scheduler scheduler = Schedulers.createScheduler();
        Path path = Paths.get(WebSocketServerDemo.class.getResource("/").toURI());

        $.httpServer()
         .router().get("/static/*").handler(new StaticFileHandler(path.toAbsolutePath().toString()))
         .router().get("/").handler(ctx -> ctx.renderTemplate("template/websocket/index.mustache"))
         .webSocket("/helloWebSocket")
         .onConnect(conn -> {
             Scheduler.Future future = scheduler.scheduleAtFixedRate(() -> conn.sendText("Current time: " + new Date()),
                     0, 1, TimeUnit.SECONDS);
             conn.onClose(c -> future.cancel());
         })
         .onText((text, conn) -> System.out.println("Server received: " + text))
         .listen("localhost", 8080);
    }
}
