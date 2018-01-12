---

category : docs
title: WebSocket server and client

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
- [WebSocket extensions](#websocket-extensions)
- [Upgrade from HTTP connection](#upgrade-from-http-connection)

<!-- /TOC -->

# Basic concepts
Unlike HTTP, WebSocket provides full-duplex communication. Additionally, WebSocket enables streams of messages on top of TCP. TCP alone deals with streams of bytes with no inherent concept of a message. Before WebSocket, port 80 full-duplex communication was attainable using Comet channels; however, Comet implementation is non-trivial, and due to the TCP handshake and HTTP header overhead, it is inefficient for small messages. WebSocket protocol aims to solve these problems without compromising security assumptions of the web.

The Firefly supports WebSockets on both the client and server-side. For example:
```java
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
```

Run it. And the console shows:
```
The client received: OK.
The server received: Hello server.
```

The WebSocket protocol specification defines `ws` and `wss` as two new uniform resource identifier (URI) schemes that are used for unencrypted and encrypted connections, respectively. Apart from the scheme name and fragment (# is not supported), the rest of the URI components are defined to use URI generic syntax.

If you want to use the TLS encrypted connection, you can use the `createSecureWebSocketServer` and `createSecureWebSocketClient` method to create the WebSocket server and client.

# WebSocket extensions
The Firefly WebSocket server and client supports some extension protocol, such as `permessage-deflate`, `deflate-frame`, `x-webkit-deflate-frame`, `fragment`, and `identity`.
```java
public static void main(String[] args) {
    SimpleWebSocketServer server = $.createWebSocketServer();
    server.webSocket("/helloWebSocket")
          .onConnect(conn -> conn.sendText("OK."))
          .onText((text, conn) -> System.out.println("The server received: " + text))
          .listen("localhost", 8080);

    SimpleWebSocketClient client = $.createWebSocketClient();
    client.webSocket("ws://localhost:8080/helloWebSocket")
          .addExtension("permessage-deflate")
          .onText((text, conn) -> System.out.println("The client received: " + text))
          .connect()
          .thenAccept(conn -> conn.sendText("Hello server."));
}
```

We can add extension using `addExtension` or `putExtension` method conveniently. Run it. And the console shows:
```
The client received: OK.
The server received: Hello server.
```


# Upgrade from HTTP connection
The WebSocket server can share the same port with HTTP server. The WebSocket use the HTTP upgrade mechanism to upgrade to the WebSocket protocol. Once the connection is established, communication switches to a bidirectional binary protocol which doesn't conform to the HTTP protocol. For example:
```java
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
```

Create WebSocket client using javascript APIs:
```html
<!DOCTYPE html>
<html>
<head>
    <title>Hello WebSocket</title>
</head>
<body>

<div>Hello WebSocket</div>
<div id="content">

</div>

<script src="/static/js/jquery-3.2.1.min.js"></script>
<script>
    var ws = new WebSocket('ws://localhost:8080/helloWebSocket');
    var f;
    ws.onopen = function () {
        f = setInterval(function() {ws.send('Hello Server!')}, 5000)
    };

    ws.onclose = function() {
        clearInterval(f);
    };

    ws.onmessage = function (event) {
        console.log(event);
        $("#content").append("<p>" + event.data + "</p>");
    };
</script>
</body>
</html>
```

Run it. And visit the url `http://localhost:8080`. The browser will print current server time continuously.
```
Hello WebSocket
Current time: Thu Jan 11 23:07:01 CST 2018

Current time: Thu Jan 11 23:07:02 CST 2018

Current time: Thu Jan 11 23:07:03 CST 2018

Current time: Thu Jan 11 23:07:04 CST 2018

Current time: Thu Jan 11 23:07:05 CST 2018

......
```

The firefly also provides Kotlin DSL APIs. For example:
```kotlin
fun main(args: Array<String>) {
    val scheduler = Schedulers.createScheduler()
    val p = Paths.get(HttpServer::class.java.getResource("/").toURI())

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            paths = listOf("/favicon.ico", "/static/*")
            handler(StaticFileHandler(p.toAbsolutePath().toString()))
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/"
            asyncHandler { renderTemplate("template/websocket/index.mustache") }
        }

        webSocket("/helloWebSocket") {
            onConnect {
                val future = scheduler.scheduleAtFixedRate(
                        { it.sendText("Current time: " + Date()) },
                        0, 1, TimeUnit.SECONDS)
                it.onClose { future.cancel() }
            }

            onText { text, _ ->
                println("Server received: " + text)
            }
        }
    }.listen("localhost", 8080)
}
```
