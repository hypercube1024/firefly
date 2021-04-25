# What is Firefly?
[![Build Status](https://travis-ci.org/hypercube1024/firefly.svg?branch=master)](https://travis-ci.org/hypercube1024/firefly)
[![Maven Central](https://img.shields.io/maven-central/v/com.fireflysource/firefly-net)](https://search.maven.org/artifact/com.fireflysource/firefly-net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Firefly framework is an asynchronous Java web framework. It helps you create a web application ***Easy*** and ***Quickly***. 
It provides asynchronous HTTP, Websocket, TCP Server/Client, and many other useful components for developing
web applications, protocol servers, etc. That means you can easy deploy your web without any other java web containers,
in short, it's containerless. Using Kotlin coroutines, Firefly is truly asynchronous and highly scalable. It taps into
the fullest potential of hardware. Use the power of non-blocking development without the callback nightmare.

Firefly core provides functionality for things like:

- HTTP server and client
- WebSocket server and client
- HTTP, Socks proxy
- HTTP Gateway
- TCP server and client
- UDP server and client

# Event driven
The Firefly APIs are largely event-driven. It means that when things happen in Firefly that you are interested in,
Firefly will call you by sending you events.

Some example events are:
- some data has arrived on a socket
- an HTTP server has received a request

Firefly handles a lot of concurrencies using just a small number of threads, so ***don't block Firefly thread***, you
must manage blocking call in the standalone thread pool.

With a conventional blocking API the calling thread might block when:
- Thread.sleep()
- Waiting on a Lock
- Waiting on a mutex or monitor
- Doing a long-lived database operation and waiting for a result
- Call blocking I/O APIs

In all the above cases, when your thread is waiting for a result it can’t do anything else - it’s effectively useless.

It means that if you want a lot of concurrencies using blocking APIs, then you need a lot of threads to prevent your
application grinding to a halt.

Threads have overhead regarding the memory they require (e.g. for their stack) and in context switching.

For the levels of concurrency required in many modern applications, a blocking approach just doesn’t scale.

# Quick start
Add maven dependency in your pom.xml.
```xml
<dependencics>
    <dependency>
        <groupId>com.fireflysource</groupId>
        <artifactId>firefly</artifactId>
        <version>5.0.0-alpha12</version>
    </dependency>

    <dependency>
        <groupId>com.fireflysource</groupId>
        <artifactId>firefly-slf4j</artifactId>
        <version>5.0.0-alpha12</version>
    </dependency>
</dependencics>
```

Add log configuration file "firefly-log.xml" to the classpath.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<loggers xmlns="http://www.fireflysource.com/loggers"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.fireflysource.com/loggers http://www.fireflysource.com/loggers.xsd">
    <logger>
        <name>firefly-system</name>
        <level>INFO</level>
        <path>logs</path>
    </logger>
</loggers>
```

HTTP server and client example:
```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx -> ctx.end("Hello http! ") }
        .listen("localhost", 8090)

    `$`.httpClient().get("http://localhost:8090/").submit()
        .thenAccept { response -> println(response.stringBody) }
}
```

WebSocket server and client example:
```kotlin
fun main() {
    `$`.httpServer().websocket("/websocket/hello")
        .onServerMessageAsync { frame, _ -> onMessage(frame) }
        .onAcceptAsync { connection -> sendMessage("Server", connection) }
        .listen("localhost", 8090)

    val url = "ws://localhost:8090"
    `$`.httpClient().websocket("$url/websocket/hello")
        .onClientMessageAsync { frame, _ -> onMessage(frame) }
        .connectAsync { connection -> sendMessage("Client", connection) }
}

private suspend fun sendMessage(data: String, connection: WebSocketConnection) = connection.useAwait {
    (1..10).forEach {
        connection.sendText("WebSocket ${data}. count: $it, time: ${Date()}")
        delay(1000)
    }
}

private fun onMessage(frame: Frame) {
    if (frame is TextFrame) {
        println(frame.payloadAsUTF8)
    }
}
```

TCP server and client example:
```kotlin
fun main() {
    `$`.tcpServer().onAcceptAsync { connection ->
        launch { writeLoop("Server", connection) }
        launch { readLoop(connection) }
    }.listen("localhost", 8090)

    `$`.tcpClient().connectAsync("localhost", 8090) { connection ->
        launch { writeLoop("Client", connection) }
        launch { readLoop(connection) }
    }
}

private suspend fun readLoop(connection: TcpConnection) = connection.useAwait {
    while (true) {
        try {
            val buffer = connection.read().await()
            println(BufferUtils.toString(buffer))
        } catch (e: Exception) {
            println("Connection closed.")
            break
        }
    }
}

private suspend fun writeLoop(data: String, connection: TcpConnection) = connection.useAwait {
    (1..10).forEach {
        connection.write(toBuffer("TCP ${data}. count: $it, time: ${Date()}"))
        delay(1000)
    }
}
```

# Contact information
- E-mail: qptkk@163.com
- QQ Group: 126079579
- Wechat: AlvinQiu
