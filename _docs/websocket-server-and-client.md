---

category : docs
title: WebSocket server and client

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
- [WebSocket extensions](#websocket-extensions)

<!-- /TOC -->

# Basic concepts
Unlike HTTP, WebSocket provides full-duplex communication. Additionally, WebSocket enables streams of messages on top of TCP. TCP alone deals with streams of bytes with no inherent concept of a message. Before WebSocket, port 80 full-duplex communication was attainable using Comet channels; however, Comet implementation is non-trivial, and due to the TCP handshake and HTTP header overhead, it is inefficient for small messages. WebSocket protocol aims to solve these problems without compromising security assumptions of the web.

The WebSocket example:
```kotlin
fun main() {
    `$`.httpServer()
        .websocket("/websocket/hello")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAcceptAsync { connection ->
            (1..10).forEach {
                connection.sendText("Server. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
        .listen("localhost", 8090)

    val url = "ws://localhost:8090"
    `$`.httpClient().websocket("$url/websocket/hello")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connectAsync { connection ->
            (1..10).forEach {
                connection.sendText("Client. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
}
```

Run it. And the console shows:
```
Server. message: 1, time: Mon Feb 15 15:35:24 CST 2021
Client. message: 1, time: Mon Feb 15 15:35:24 CST 2021
Server. message: 2, time: Mon Feb 15 15:35:25 CST 2021
Client. message: 2, time: Mon Feb 15 15:35:25 CST 2021
Server. message: 3, time: Mon Feb 15 15:35:26 CST 2021
Client. message: 3, time: Mon Feb 15 15:35:26 CST 2021

......
```

The WebSocket protocol specification defines `ws` and `wss` as two new uniform resource identifier (URI) schemes that are used for unencrypted and encrypted connections, respectively. Apart from the scheme name and fragment (# is not supported), the rest of the URI components are defined to use URI generic syntax.

# WebSocket extensions
The Firefly WebSocket server and client supports some extension protocol, such as `permessage-deflate`, `deflate-frame`, `x-webkit-deflate-frame`, `fragment`, and `identity`.
```kotlin
fun main() {
    `$`.httpServer()
        .websocket("/websocket/hello")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAcceptAsync { connection ->
            (1..10).forEach {
                connection.sendText("Server. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
        .listen("localhost", 8090)

    val url = "ws://localhost:8090"
    `$`.httpClient().websocket("$url/websocket/hello")
        .extensions(listOf("permessage-deflate")) // (1)
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connectAsync { connection ->
            (1..10).forEach {
                connection.sendText("Client. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
}
```

We can add extension using `extensions` method. Run it. And the console shows: