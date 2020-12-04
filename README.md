# What is Firefly?
[![Build Status](https://travis-ci.org/hypercube1024/firefly.svg?branch=master)](https://travis-ci.org/hypercube1024/firefly)
[![Maven Central](https://img.shields.io/maven-central/v/com.fireflysource/firefly-net)](https://search.maven.org/artifact/com.fireflysource/firefly-net/5.0.0-alpha1/jar)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Firefly framework is an asynchronous Java web framework. It helps you create a web application ***Easy*** and ***Quickly***. 
It provides asynchronous HTTP, Websocket, TCP Server/Client, and many other useful components for developing web applications, protocol servers, etc. 
That means you can easy deploy your web without any other java web containers, in short, it's containerless. 
Using Kotlin coroutines, Firefly is truly asynchronous and highly scalable. 
It taps into the fullest potential of hardware. Use the power of non-blocking development without the callback nightmare.

Firefly core provides functionality for things like:
- TCP client and server
- HTTP client and server
- WebSocket client and server
- HTTP, Socks proxy
- HTTP Gateway

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
        <version>5.0.0-alpha4</version>
    </dependency>

    <dependency>
        <groupId>com.fireflysource</groupId>
        <artifactId>firefly-slf4j</artifactId>
        <version>5.0.0-alpha4</version>
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
        <path>${log.path}</path>
    </logger>

    <logger>
        <name>firefly-monitor</name>
        <level>INFO</level>
        <path>${log.path}</path>
    </logger>
</loggers>
```

Create the HTTP server and client
```kotlin
fun main() = runBlocking {
    val httpServer = HttpServerFactory.create()
    httpServer
        .router().get("/test").handler {
            it.end("Welcome")
        }
        .listen("localhost", 9999)

    val client = HttpClientFactory.create()
    val response = client.get("http://localhost:9999/test").submit().await()
    println(response.status)
    println(response.stringBody)
}
```

Create WebSocket server and client
```kotlin
fun main() = runBlocking {
    val server = HttpServerFactory.create()
    server
        .websocket("/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAccept { connection ->
            connection.coroutineScope.launch {
                while (true) {
                    connection.sendText("Server time: ${Date()}")
                    delay(1000)
                }
            }
            Result.DONE
        }
        .listen("localhost", 8999)

    val client = HttpClientFactory.create()
    val webSocketConnection = client
        .websocket("ws://localhost:8999/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connect()
        .await()
    launch {
        while (true) {
            delay(2000)
            webSocketConnection.sendText("Client time: ${Date()}")
        }
    }
    Unit
}
```

# Contact information
- E-mail: qptkk@163.com
- QQ Group: 126079579
- Wechat: AlvinQiu
