package com.firefly.example.webscoket;

import com.firefly.$;
import com.firefly.client.websocket.SimpleWebSocketClient;
import com.firefly.server.websocket.SimpleWebSocketServer;

/**
 * @author Pengtao Qiu
 */
public class WebSocketServerAndClientDemo {

    public static void main(String[] args) {
        SimpleWebSocketServer server = $.createWebSocketServer();
        server.webSocket("/helloWebSocket")
              .onConnect(conn -> conn.sendText("OK."))
              .onText((text, conn) -> System.out.println("The server received: " + text))
              .listen("localhost", 8080);

        SimpleWebSocketClient client = $.createWebSocketClient();
        client.webSocket("ws://localhost:8080/helloWebSocket")
              .onText((text, conn) -> System.out.println("The client received: " + text))
              .connect()
              .thenAccept(conn -> conn.sendText("Hello server."));
    }
}
