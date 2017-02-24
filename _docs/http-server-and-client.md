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
- [Routing by paths with wildcard](#routing-by-paths-with-wildcard)
- [Routing with regular expressions](#routing-with-regular-expressions)
- [Routing by HTTP method](#routing-by-http-method)
- [Routing based on MIME type of request](#routing-based-on-mime-type-of-request)
- [Routing based on MIME types acceptable by the client](#routing-based-on-mime-types-acceptable-by-the-client)
- [Combining routing criteria](#combining-routing-criteria)
- [Error handling](#error-handling)
- [Handling sessions](#handling-sessions)
- [Serving static resources](#serving-static-resources)
- [Rendering template](#rendering-template)
- [Multipart file uploading](#multipart-file-uploading)

<!-- /TOC -->

# Basic concepts
The router is one of the core concepts of Firefly HTTP server. It maintains zero or more Routes.

A router takes an HTTP request and finds the first matching route for that request, and passes the request to that route.

The route can have a handler associated with it, which then receives the request. You then do something with the request, and then, either end it or pass it to the next matching handler.

Here’s a simple router example:
```java
public class HelloHTTPServerAndClient {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(2);

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

It uses fluent style API to help you to build complex application.

Chaining calls like this allows you to write code that’s a little bit less verbose. Of course, if you don’t like the fluent approach we don’t force you to do it that way, you can happily ignore it if you prefer and write your code using object-oriented (OO) style API. The following sections explain how to use them.

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
It’s possible to match paths using placeholders for parameters. The placeholders consist of : followed by the parameter name. Parameter names consist of any alphabetic character, numeric character or underscore.

```java
public class CapturingPathParameterDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/good/:type/:id")
         .handler(ctx -> {
            String type = ctx.getRouterParameter("type");
            String id = ctx.getRouterParameter("id");
            ctx.end("get good type: " + type + ", id: " + id);
         }).listen("localhost", 8080);
    }
}
```
In the above example, if a GET request is made to path: "http://localhost:8080/good/fruit/3" then the route will match and type will receive the value "fruit" and id will receive the value "3".
```
get good type: fruit, id: 3
```

# Routing by exact path
A route can be set-up to match the path from the request URI. In this case it will match any request which has a path that’s the same as the specified path.

In the following example the handler will be called for a request /product/tools/. We also ignore trailing slashes so it will be called for paths /product/tools and /product/tools/ too:
```java
public class RoutingByExactPathDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/product/tools")
         .handler(ctx -> ctx.write("spanner: 3").write("\r\n")
                            .write("pliers: 1").write("\r\n")
                            .end("screwdriver: 1"))
         .listen("localhost", 8080);
    }
}
```
Visit the url "http://localhost:8080/product/tools" or "http://localhost:8080/product/tools/"
```
spanner: 3
pliers: 1
screwdriver: 1
```

# Routing by paths with wildcard
Often you want to route all requests that accord with a pattern. You could use a regex to do this, but a simply way is to use an asterisk wildcard *

```java
public class RoutingByPathsWithWildcardDemo {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/product*")
         .handler(ctx -> ctx.end("current path is " + ctx.getURI().getPath()))
         .router().get("/*items*")
         .handler(ctx -> ctx.end("current path is " + ctx.getURI().getPath()))
         .listen("localhost", 8080);
    }
}
```

For example "http://localhost:8080/product/apple" and "http://localhost:8080/good/my-items" would both match.
```
current path is /product/apple

current path is /good/my-items
```

# Routing with regular expressions
Regular expressions can also be used to match URI paths in routes.
```java
public class RoutingWithRegexDemo {
    public static void main(String[] args) {
        $.httpServer().router()
         .method(HttpMethod.GET).pathRegex("/hello(\\d*)")
         .handler(ctx -> {
             String group1 = ctx.getRouterParameter("group1");
             ctx.write("match path: " + ctx.getURI().getPath()).write("\r\n")
                .end("capture group1: " + group1);
         }).listen("localhost", 8080);
    }
}
```

Visit "http://localhost:8080/hello55", the server response:

```
match path: /hello55
capture group1: 55
```
In the above example, we can get the values of regex capture group, use method **ctx.getRouterParameter** and the parameter format is "group{index}".


# Routing by HTTP method
By the default, a route will match all HTTP methods if you do not call **router.method**, **router.get**, **router.post**, **router.put** or **router.delete**.

```java
public class RoutingByAllHTTPmethodDemo {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().path("/all-methods")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .listen("localhost", 8080);

        $.httpClient().post("http://localhost:8080/all-methods").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().put("http://localhost:8080/all-methods").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
```

In this example, we only call **router.path** to bind path "/all-methods", it match all HTTP methods, run it result:

```
the HTTP method: PUT
the HTTP method: POST
```

Calling **router.method** specifies a HTTP method to a router, like this:

```java
public class RoutingBySpecifiedHTTPmethodDemo {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(4);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().method(HttpMethod.GET).path("/get-or-post")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .router().post("/get-or-post")
              .handler(ctx -> ctx.end("the HTTP method: " + ctx.getMethod()))
              .listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().post("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.httpClient().put("http://localhost:8080/get-or-post").submit()
         .thenAccept(res -> {
             System.out.println(res.getStatus() + " " + res.getReason());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
```
The shortcut method **router.post("/get-or-post")** (or get, put, delete) equals **router.method("POST").path("/get-or-post")**, run it result:

```
404 Not Found
the HTTP method: POST
the HTTP method: GET
```

In the above example, the path "http://localhost:8080/get-or-post" only receives GET or POST method, the PUT request is not matched.

# Routing based on MIME type of request
You can specify that a route will match against matching request MIME types using **router.consumes**.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, consumes is describing which MIME types the handler can consume.

Matching can be done on exact MIME type matches:

```java
public class RoutingByContentTypeDemo {

    public static class Product {
        public int id;
        public String name;

        @Override
        public String toString() {
            return "id[" + id + "], name[" + name + "]";
        }
    }

    public static void main(String[] args) throws Exception {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().put("/product/:id").consumes("application/json")
              .handler(ctx -> {
                  Product product = ctx.getJsonBody(Product.class);
                  ctx.end("update product: " + product + " success");
              })
              .router().post("/product").consumes("*/json")
              .handler(ctx -> {
                  Product product = ctx.getJsonBody(Product.class);
                  ctx.write("content type: " + ctx.getRouterParameter("param0"))
                     .write("\r\n")
                     .end("create product: " + product + " success");
              }).listen("localhost", 8080);

        Product product = new Product();
        product.name = "new book";
        Completable<SimpleResponse> c = $.httpClient().post("http://localhost:8080/product")
                                         .jsonBody(product)
                                         .submit();
        System.out.println(c.get().getStringBody());

        product = new Product();
        product.id = 1;
        product.name = "old book";
        $.httpClient().put("http://localhost:8080/product/1").jsonBody(product).submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
```

The router specifies content-type "application/json", that means the server can only handle json content, also you use the wildcard in the MIME type, run it result:

```
content type: application
create product: id[0], name[new book] success
update product: id[1], name[old book] success
```

# Routing based on MIME types acceptable by the client
The HTTP accept header is used to signify which MIME types of the response are acceptable to the client.

An accept header can have multiple MIME types separated by ‘,’.

MIME types can also have a q value appended to them* which signifies a weighting to apply if more than one response MIME type is available matching the accept header. The q value is a number between 0 and 1.0. If omitted it defaults to 1.0.

For example, the following accept header signifies the client will accept a MIME type of only text/plain:
```
Accept: text/plain
```
With the following the client will accept text/plain or text/html with no preference.
```
Accept: text/plain, text/html
```
With the following the client will accept text/plain or text/html but prefers text/html as it has a higher q value (the default value is q=1.0)
```
Accept: text/plain; q=0.9, text/html
```
If the server can provide both text/plain and text/html it should provide the text/html in this case.

By using **router.produces** you define which MIME type(s) the route produces, e.g. the following handler produces a response with MIME type application/json.

```java
public class RoutingByAcceptDemo {

    public static class Apple {
        public String color;
        public double weight;

        @Override
        public String toString() {
            return "color[" + color + "], weight[" + weight + "]";
        }
    }

    public static void main(String[] args) {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().get("/apple/:id").produces("application/json")
              .handler(ctx -> {
                  Apple apple = new Apple();
                  apple.weight = 1.2;
                  apple.color = "red";
                  ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.asString())
                     .end($.json.toJson(apple));
              }).listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/apple/1")
         .put(HttpHeader.ACCEPT, "text/plain; q=0.9, application/json").submit()
         .thenAccept(res -> {
             System.out.println(res.getJsonBody(Apple.class));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
```

In this case the route will match with any request with an accept header that matches application/json.

Here are some examples of accept headers that will match:
```
Accept: application/json  
Accept: application/*  
Accept: application/json, text/html  
Accept: application/json;q=0.7, text/html;q=0.8, text/plain  
```

# Combining routing criteria
You can combine all the above routing criteria in many different ways, for example:

```java
public class CombiningRoutingCriteriaDemo {
    public static class Task {
        public String name;
        public Date date;

        public String toString() {
            return "name:[" + name + "], date[" + date + "]";
        }
    }

    public static void main(String[] args) {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder server = $.httpServer();
        server.router().post("/task/create")
              .produces("application/json").consumes("*/json")
              .handler(ctx -> {
                  Map<String, Object> ret = new HashMap<>();
                  ret.put("msg", "create task, " + ctx.getJsonBody(Task.class) + " success ");
                  ret.put("code", 0);
                  ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.asString())
                     .end($.json.toJson(ret));
              }).listen("localhost", 8080);

        Task task = new Task();
        task.name = "TODO today";
        task.date = new Date();
        $.httpClient().post("http://localhost:8080/task/create")
         .put(HttpHeader.ACCEPT, "text/plain; q=0.9, application/json")
         .jsonBody(task).submit()
         .thenAccept(res -> {
             System.out.println(res.getJsonObjectBody());
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        server.stop();
        $.httpClient().stop();
    }
}
```

The **httpclient.jsonBody** puts the "Content-Type: application/json" header implicitly, run it result:

```text
{msg=create task, name:[TODO today], date[Fri Feb 24 16:59:33 CST 2017] success , code=0}
```  

# Error handling
We provide default failure handler **com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandler**  to handle some errors.

```java
public class ErrorHandlerDemo {
    public static void main(String[] args) {
        $.httpServer().router().get("/error")
         .handler(ctx -> {
             throw new CommonRuntimeException("perhaps some errors happen");
         }).listen("localhost", 8080);
    }
}
```

Visit "http://localhost:8080/error" and view:

```
500 Server Error

The server internal error.
perhaps some errors happen

powered by Firefly 4.0.21
```

As well as setting handlers to handle requests you can also set handlers to handle failures in routing. just like:

```java
$.httpServer().router().path("*")
 .handler(ctx -> {
  if (ctx.hasNext()) {
    try {
      ctx.next();
    } catch (Exception e) {
      // response some error information
    }
  } else {
      // response 404
  }
})
```

Also you can extend **com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler** conveniently and sets some routing criteria to handle failure.


# Handling sessions

# Serving static resources

# Rendering template

# Multipart file uploading
