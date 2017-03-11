---

category : docs
layout: document
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
    <version>4.0.21</version>
</dependency>

<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-slf4j</artifactId>
    <version>4.0.21</version>
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

Create a HTTP server in main function
```java
public class HelloHTTPServer {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }
}
```

Run and view
```
http://localhost:8080/
```

More detailed information, please refer to the [full document](http://www.fireflysource.com/docs/http-server-and-client.html)
and [example project](https://github.com/hypercube1024/firefly/tree/master/firefly-example).
