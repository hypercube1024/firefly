---

category : docs
title: Getting Started

---

**Table of Contents**

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [What is Firefly?](#what-is-firefly)
- [Event driven](#event-driven)
- [Quick start](#quick-start)

<!-- /TOC -->

# What is Firefly?  

[![Build Status](https://travis-ci.org/hypercube1024/firefly.svg?branch=master)](https://travis-ci.org/hypercube1024/firefly)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fireflysource/firefly/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fireflysource/firefly)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Firefly framework is an asynchronous Java web framework. It helps you create a web application ***Easy*** and ***Quickly***.
It provides MVC framework, asynchronous HTTP Server/Client, asynchronous TCP Server/Client and many other useful components for developing web applications, protocol servers, etc.
That means you can easy deploy your web without any other java web containers, in short, it's containerless.
It taps into the fullest potential of hardware using ***SEDA*** architecture, a highly customizable thread model.

Firefly core provides functionality for things like:
- Writing TCP clients and servers
- Writing HTTP clients and servers
- Writing WebSocket clients and servers
- Writing web application with MVC framework and template engine
- Database access

# Event driven

The Firefly APIs are largely event-driven. It means that when things happen in Firefly that you are interested in, Firefly will call you by sending you events.

Some example events are:
- some data has arrived on a socket
- an HTTP server has received a request

Firefly handles a lot of concurrencies using just a small number of threads,
so ***don't block Firefly thread***, you must manage blocking call in the standalone thread pool.

With a conventional blocking API the calling thread might block when:
- Thread.sleep()
- Waiting on a Lock
- Waiting on a mutex or monitor
- Doing a long-lived database operation and waiting for a result
- Call blocking I/O APIs

In all the above cases, when your thread is waiting for a result it can’t do anything else - it’s effectively useless.

It means that if you want a lot of concurrencies using blocking APIs, then you need a lot of threads to prevent your application grinding to a halt.

Threads have overhead regarding the memory they require (e.g. for their stack) and in context switching.

For the levels of concurrency required in many modern applications, a blocking approach just doesn’t scale.

# Quick start

Add maven dependency in your pom.xml.
```xml
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

Create an HTTP server (Java version)
```java
public class HelloHTTPServer {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }
}
```

Create an HTTP client (Java version)
```java
public class HelloHTTPClient {
    public static void main(String[] args) {
        $.httpClient().get("http://localhost:8080/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()));
    }
}
```

Create WebSocket server and client (Java version)
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

Firefly also supports to create HTTP server/client using Kotlin DSL.  

Add maven dependency in your pom.xml
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-kotlin-ext</artifactId>
    <version>{{ site.data.global.releaseVersion }}</version>
</dependency>
```

Create an HTTP server (Kotlin DSL version)
```kotlin
fun main(args: Array<String>) {
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler {
                end("hello world!")
            }
        }
    }.listen("localhost", 8080)
}
```

Create an HTTP client (Kotlin coroutine asynchronous client)
```kotlin
fun main(args: Array<String>) = runBlocking {
    val msg = firefly.httpClient().get("http://localhost:8080").asyncSubmit().stringBody
    println(msg)
}
```

Create WebSocket server and client (Kotlin version)
```kotlin
fun main(args: Array<String>) {
    val server = firefly.createWebSocketServer()
    server.webSocket("/helloWebSocket")
          .onConnect { conn -> conn.sendText("OK.") }
          .onText { text, conn -> println("The server received: " + text) }
          .listen("localhost", 8080)

    val client = firefly.createWebSocketClient()
    client.webSocket("ws://localhost:8080/helloWebSocket")
          .onText { text, conn -> println("The client received: " + text) }
          .connect()
          .thenAccept { conn -> conn.sendText("Hello server.") }
}
```

More detailed information, please refer to the
* [HTTP server/client document]({{ site.url }}/docs/http-server-and-client.html)
* [WebSocket server and client]({{ site.url }}/docs/websocket-server-and-client.html)
* [TCP server/client document]({{ site.url }}/docs/tcp-server-and-client.html)
* [SSL/TLS configuration document]({{ site.url }}/docs/ssl-tls-configuration.html)
* [Inversion of control document]({{ site.url }}/docs/ioc-framework.html)
* [Database access document]({{ site.url }}/docs/database-access.html)
* [Log document]({{ site.url }}/docs/log.html)
* [OAuth2 server/client document]({{ site.url }}/docs/oauth2-server-and-client.html)
* [CLI generator document]({{ site.url }}/docs/cli-generator.html)
* [HTTP server/client document (Kotlin version)]({{ site.url }}/docs/http-server-and-client-kotlin-ext.html)
* [Database access document (Kotlin version)]({{ site.url }}/docs/database-access-kotlin.html)
* [Example (Java version)]({{ site.data.global.githubURL }}/tree/master/firefly-example/src/main/java/com/firefly/example)
* [Example (Kotlin version)]({{ site.data.global.githubURL }}/tree/master/firefly-example/src/main/kotlin/com/firefly/example/kotlin)

# Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579  

Treat author to a cup of coffee:  
<img src="{{ site.url }}/images/author-ali-pay.jpg" width="260" />
<img src="{{ site.url }}/images/author-wechat-pay.jpg" width="260" />

Get the red envelope:  
<img src="{{ site.url }}/images/author-red-envelope.png" width="260" />
