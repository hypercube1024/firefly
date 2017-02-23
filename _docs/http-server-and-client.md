---

category : docs
layout: document
title: HTTP server and client

---
**Table of Contents**
<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
	- [Class $](#class-)
	- [Route order](#route-order)
	- [Calling the next handler](#calling-the-next-handler)
	- [Routing Context](#routing-context)
- [Capturing path parameters](#capturing-path-parameters)
- [Routing by exact path](#routing-by-exact-path)
- [Routing by paths that begin with something](#routing-by-paths-that-begin-with-something)
- [Routing with regular expressions](#routing-with-regular-expressions)
- [Capturing path parameters with regular expressions](#capturing-path-parameters-with-regular-expressions)
- [Routing by HTTP method](#routing-by-http-method)
- [Routing based on MIME type of request](#routing-based-on-mime-type-of-request)
- [Routing based on MIME types acceptable by the client](#routing-based-on-mime-types-acceptable-by-the-client)
- [Combining routing criteria](#combining-routing-criteria)
- [Error handling](#error-handling)
- [Handling sessions](#handling-sessions)
- [Serving static resources](#serving-static-resources)
- [CORS handling](#cors-handling)
- [Template](#template)

<!-- /TOC -->

# Basic concepts
The router is one of the core concepts of Firefly HTTP server. It maintains zero or more Routes.

A router takes an HTTP request and finds the first matching route for that request, and passes the request to that route.

The route can have a handler associated with it, which then receives the request. You then do something with the request, and then, either end it or pass it to the next matching handler.

Here’s a simple router example:
```java
public class HelloHTTPServerAndClient {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/").handler(ctx -> ctx.write("hello world! ").next())
                  .router().get("/").handler(ctx -> ctx.end("end message"))
                  .listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}
```

It basically does the same thing as the HTTP server hello world example from the previous section,
but this time using two routers handle GET method and path '/'.

## Class $
The class $ provides primary API of Firefly, such as
- Creating HTTP server and client
- Creating TCP server and client
- Creating application context
- I/O, string, json, thread ... utilities

## Route order
By default routes are matched in the order they are added to the router manager. **httpServer.router()** creates a router and adds it to the router manager.
When a request arrives the router will step through each route and check if it matches, if it matches then the handler for that route will be called.

## Calling the next handler
If the handler subsequently calls **ctx.next()** the handler for the next matching route (if any) will be called. And so on.

In the above example the response will contain:
```
hello world! end message
```

## Routing Context
A new RoutingContext(ctx) instance is created for each HTTP request.

You can visit the RoutingContext instance in the whole router chain. It provides HTTP request/response API and allows you to maintain arbitrary data that lives for the lifetime of the context. Contexts are discarded once they have been routed to the handler for the request.

The context also provides access to the Session, cookies and body for the request, given the correct handlers in the application.

Here’s an example where one handler sets some data in the context data and a subsequent handler retrieves it:

You can use the **ctx.setAttribute** to put any object, and **ctx.getAttribute** to retrieve any object from the context data.

```java
public class ContextDataSharingDemo {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/data/foo")
         .handler(ctx -> {
             ctx.setAttribute("fooData", "I'm foo");
             ctx.write("foo sets an attribute").write("\r\n").next();
         })
         .router().get("/data/:other")
         .handler(ctx -> ctx.write((String) ctx.getAttribute("fooData"))
                            .write("\r\n")
                            .end(ctx.getRouterParameter("other") + " is coming"))
         .listen("localhost", 8080);
    }
}
```
You visit the "http://localhost:8080/data/foo" , the server will response:
```
foo sets an attribute
I'm foo
foo is coming
```

If you visit the "http://localhost:8080/data/bar" , the server will response:
```
null
bar is coming
```

In this case, the second router handles GET method and match paths using placeholders for parameters which are then available in the **ctx.getRouterParameter**.
We will show how to capture the path parameters in the next chapter.


# Capturing path parameters

# Routing by exact path

# Routing by paths that begin with something

# Routing with regular expressions

# Capturing path parameters with regular expressions

# Routing by HTTP method

# Routing based on MIME type of request

# Routing based on MIME types acceptable by the client

# Combining routing criteria

# Error handling

# Handling sessions

# Serving static resources

# CORS handling

# Template
