---

category : docs
title: Getting Started

---

**Table of Contents**

- [What is Firefly?](#what-is-firefly)
- [Event driven](#event-driven)
- [Quick start](#quick-start)
- [Contact information](#contact-information)

<a id="markdown-what-is-firefly" name="what-is-firefly"></a>
# What is Firefly?
[![Build Status](https://travis-ci.org/hypercube1024/firefly.svg?branch=master)](https://travis-ci.org/hypercube1024/firefly)
[![Maven Central](https://img.shields.io/maven-central/v/com.fireflysource/firefly-net)](https://search.maven.org/artifact/com.fireflysource/firefly-net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Firefly framework is an asynchronous Java web framework. It helps you create a web application ***Easy*** and ***Quickly***. 
It provides asynchronous HTTP, Websocket, TCP Server/Client, and many other useful components for developing web applications, protocol servers, etc. 
That means you can easy deploy your web without any other java web containers, in short, it's containerless. 
Using Kotlin coroutines, Firefly is truly asynchronous and highly scalable. 
It taps into the fullest potential of hardware. Use the power of non-blocking development without the callback nightmare.

Firefly core provides functionality for things like:
- HTTP server and client
- WebSocket server and client
- HTTP, Socks proxy
- HTTP Gateway
- TCP server and client
- UDP server and client

<a id="markdown-event-driven" name="event-driven"></a>
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

<a id="markdown-quick-start" name="quick-start"></a>
# Quick start
Add maven dependency in your pom.xml.
```xml
<dependencics>
    <dependency>
        <groupId>com.fireflysource</groupId>
        <artifactId>firefly</artifactId>
        <version>{{ site.data.global.releaseVersion }}</version>
    </dependency>

    <dependency>
        <groupId>com.fireflysource</groupId>
        <artifactId>firefly-slf4j</artifactId>
        <version>{{ site.data.global.releaseVersion }}</version>
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

Create the HTTP server and client
```kotlin
fun main() = runBlocking {
    val httpServer = HttpServerFactory.create()
    httpServer
        .router().get("/test").handler { it.end("Welcome") }
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
    val host = "localhost"
    val port = 8999
    val server = HttpServerFactory.create()
    server
        .websocket("/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAcceptAsync { connection ->
            var id = 1
            while (true) {
                if (id < 10) {
                    connection.sendText("$id Server time: ${Date()}")
                    id += 2
                    delay(2000)
                } else break
            }
            connection.closeAsync().await()
        }
        .listen(host, port)

    val client = HttpClientFactory.create()
    val webSocketConnection = client.websocket("ws://$host:$port/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connect()
        .await()

    webSocketConnection.coroutineScope.launch {
        var id = 0
        while (true) {
            if (id < 10) {
                webSocketConnection.sendText("$id Client time: ${Date()}")
                id += 2
                delay(2000)
            } else break
        }
        webSocketConnection.closeAsync().await()
    }

    Unit
}
```

More detailed information, please refer to the
* [HTTP server/client document]({{ site.url }}/docs/http-server-and-client.html)
* [WebSocket server and client]({{ site.url }}/docs/websocket-server-and-client.html)
* [TCP server/client document]({{ site.url }}/docs/tcp-server-and-client.html)
* [SSL/TLS configuration document]({{ site.url }}/docs/ssl-tls-configuration.html)
* [Log document]({{ site.url }}/docs/log.html)

<a id="markdown-contact-information" name="contact-information"></a>
# Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579  
