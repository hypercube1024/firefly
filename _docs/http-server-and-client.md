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
	- [Local session store](#local-session-store)
	- [Remote session store](#remote-session-store)
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

It does the same thing as the HTTP server hello world example from the previous section,
but this time using two routers handle GET method and path '/'.

## Class $
The class $ provides primary API of Firefly, such as
- Creating HTTP server and client
- Creating TCP server and client
- Creating application context
- I/O, string, JSON, thread ... utilities

It uses fluent style API to help you to build a complex application.

Chaining calls like this allow you to write code that’s a little bit less verbose. Of course, if you don’t like the fluent approach we don’t force you to do it that way, you can happily ignore it if you prefer and write your code using object-oriented (OO) style API. The following sections explain how to use them.

## Route order
By default routes are matched in the order they are added to the router manager. **httpServer.router()** creates a router and adds it to the router manager.
When a request arrives, the router will step through each route and check if it matches then the handler for that route will be called.

## Calling the next handler
If the handler subsequently calls **ctx.next()** the handler for the next matching route (if any) will be called. And so on.

In the above example the response will contain:
```
hello world! end message
```

## Routing Context
A new RoutingContext(ctx) instance is created for each HTTP request.

You can visit the RoutingContext instance in the whole router chain. It provides HTTP request/response API and allows you to maintain arbitrary data that lives for the lifetime of the context. Contexts are discarded once they have been routed to the handler for the request.

The context also provides access to the Session, cookies, and body for the request, given the correct handlers in the application.

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
In the above example, if a GET request is made to path: "http://localhost:8080/good/fruit/3" then the type will receive the value "fruit" and id will receive the value "3".
```
get good type: fruit, id: 3
```

# Routing by exact path
A route can be set-up to match the path from the request URI. In this case, it will match any request which has a path that’s the same as the specified path.

In the following example, the handler will be called for a request /product/tools/. We also ignore trailing slashes, so it will be called for paths /product/tools and /product/tools/ too:
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

Calling **router.method** specifies an HTTP method to a router, like this:

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

In the above example, the path "http://localhost:8080/get-or-post" only receives GET or POST method. The PUT request is not matched.

# Routing based on MIME type of request
You can specify that a route will match against matching request MIME types using **router.consumes**.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, **router.consumes** is describing which MIME types the handler can consume.

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

The router specifies content-type "application/json", that means the server can only handle JSON content. Also, you use the wildcard in the MIME type, run it result:

```
content type: application
create product: id[0], name[new book] success
update product: id[1], name[old book] success
```

# Routing based on MIME types acceptable by the client
The HTTP Accept header is used to signify which MIME types of the response are acceptable to the client.

An accept header can have multiple MIME types separated by ‘,’.

MIME types can also have a q value appended to them* which signifies a weighting to apply if more than one response MIME type is available matching the HTTP Accept header. The q value is a number between 0 and 1.0. If omitted it defaults to 1.0.

For example, the following accept header signifies the client will accept a MIME type of only text/plain:
```
Accept: text/plain
```
With the following, the client will accept text/plain or text/html with no preference.
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

In this case, the route will match with any request with an HTTP Accept header that matches application/json.

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
Firefly uses session cookies to identify a session. The session cookie is temporary and will be deleted by your browser when it’s closed.

We don’t put the actual data of your session in the session cookie - the cookie simply uses an identifier to look-up the actual session on the server. The identifier is a random UUID generated using a secure random, so it should be effectively unguessable.

Cookies are passed across the wire in HTTP requests and responses so we recommend you to enable HTTPS when sessions are being used.

## Local session store

In this case, we use the local session store. Sessions are stored locally in memory and only available in this instance.

This store is appropriate if you have just a single Firefly instance of you are using sticky sessions in your application and have configured your load balancer to always route HTTP requests to the same Firefly instance.

```java
public class LocalSessionDemo {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8081;
        String uri = "https://" + host + ":" + port;

        int maxGetSession = 3;
        Phaser phaser = new Phaser(1 + maxGetSession + 1);

        HTTP2ServerBuilder httpsServer = $.httpsServer();
        LocalHTTPSessionHandler sessionHandler = new LocalHTTPSessionHandler(new HTTPSessionConfiguration());
        httpsServer.router().path("*").handler(sessionHandler)
                   .router().path("*").handler(new DefaultErrorResponseHandler())
                   .router().post("/session/:name")
                   .handler(ctx -> {
                       String name = ctx.getRouterParameter("name");
                       System.out.println("the path param -> " + name);
                       HttpSession session = ctx.getSession(true);
                       session.setAttribute(name, "bar");
                       // 1 second later, the session will expire
                       session.setMaxInactiveInterval(1);
                       ctx.end("create session success");
                   })
                   .router().get("/session/:name")
                   .handler(ctx -> {
                       HttpSession session = ctx.getSession();
                       if (session != null) {
                           ctx.end("session value is " + session.getAttribute("foo"));
                       } else {
                           ctx.end("session is invalid");
                       }
                   })
                   .listen(host, port);

        List<Cookie> c
                = $.httpsClient().post(uri + "/session/foo").submit()
                   .thenApply(res -> {
                       List<Cookie> cookies = res.getCookies();
                       System.out.println(res.getStatus());
                       System.out.println(cookies);
                       System.out.println(res.getStringBody());
                       return cookies;
                   })
                   .thenApply(cookies -> {
                       for (int i = 0; i < maxGetSession; i++) {
                           $.httpsClient().get(uri + "/session/foo").cookies(cookies).submit()
                            .thenAccept(res2 -> {
                                String sessionFoo = res2.getStringBody();
                                System.out.println(sessionFoo);
                                phaser.arrive();
                            });
                       }
                       return cookies;
                   }).get();

        $.thread.sleep(3000L); // the session expired
        $.httpsClient().get(uri + "/session/foo").cookies(c).submit()
         .thenAccept(res -> {
             String sessionFoo = res.getStringBody();
             System.out.println(sessionFoo);
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpsServer.stop();
        $.httpsClient().stop();
        sessionHandler.stop();
    }
}
```

In above example, we show how to use local session store and set session expired time, run it result:

```
the path param -> foo
200
[Cookie [name=jsessionid, value=2df1970e-62c4-4da6-9a7d-0fd874309f14, comment=null, domain=null, maxAge=-1, path=/, secure=false, version=0, isHttpOnly=false]]
create session success
session value is bar
session value is bar
session value is bar
session is invalid
```

## Remote session store
Usually, we store session in a distributed map which is accessible across the Firefly cluster, such as Redis, Memcached and so on.

Just two steps:
- Implement "com.firefly.server.http2.router.handler.session.SessionStore", and store session in a remote map.
- Extend "com.firefly.server.http2.router.handler.session.AbstractSessionHandler", and implement factory method "createSessionStore".

```java
public class RemoteHTTPSessionHandler extends AbstractSessionHandler {

    public RemoteHTTPSessionHandler(HTTPSessionConfiguration configuration) {
        super(configuration);
    }

    @Override
    public SessionStore createSessionStore() {
        return new RemoteSessionStore();
    }

}
```

# Serving static resources
Firefly comes with an out of the box handler for serving static web resources so you can write static web servers very easily.

To serve static resources such as .html, .css, .js or any other static resource, you use an instance of "StaticFileHandler".

Any requests to paths handled by the static handler will result in files being served from a directory on the file system or from the classpath.

In the following example, all requests to paths starting with /static/ will get served from the classpath.

```java
public class StaticFileDemo {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(StaticFileDemo.class.getResource("/").toURI());
        $.httpServer().router().get("/static/*")
         .handler(new StaticFileHandler(path.toAbsolutePath().toString()))
         .listen("localhost", 8080);
    }
}
```

For example, if there was a request with path "/static/poem.html" the StaticFileHandler will look for a file in the directory "{classpath}/static/poem.html".

You can find this demo in the project "firefly-example", run and view:
```
http://localhost:8080/static/poem.html
```

# Rendering template
Firefly includes dynamic page generation capabilities by including out of the box support for Mustache template engine. You can also easily add your own.

The ideal template engine contains a list of requirements, like this:
- Very little or no business logic in the templates
- Composable components, not monolithic pages
- Allows mock data within the template that is replaced at runtime
- Works well with HTML5/CSS3 progressive enhancement

Right now I am looking at mustache.js and its various server-side implementations as a possible solution to this. [Mustache.java](https://github.com/spullara/mustache.java) is a derivative of [mustache.js](http://mustache.github.io/mustache.5.html).

```java
public class TemplateDemo {
    public static class Example {

        List<Item> items() {
            return Arrays.asList(
                    new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
                    new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
            );
        }

        static class Item {
            Item(String name, String price, List<Feature> features) {
                this.name = name;
                this.price = price;
                this.features = features;
            }

            String name, price;
            List<Feature> features;
        }

        static class Feature {
            Feature(String description) {
                this.description = description;
            }

            String description;
        }

    }

    public static void main(String[] args) {
        $.httpServer().router().get("/example").handler(ctx -> {
            ctx.put(HttpHeader.CONTENT_TYPE, "text/plain")
               .renderTemplate("template/example.mustache", new Example());
        }).listen("localhost", 8080);
    }
}
```

The Mustache template file is at "{classpath}/template/example.mustache".

```mustache
{% raw %}
{{#items}}
Name: {{name}}
Price: {{price}}
{{#features}}
    Feature: {{description}}
{{/features}}
{{/items}}
{% endraw %}
```

Run it and view:
```
http://localhost:8080/example
```

# Multipart file uploading

```java
public class MultipartDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        String uri = "http://" + host + ":" + port;
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/upload/string").handler(ctx -> {
            // small multi part data test case
            Part test1 = ctx.getPart("test1");
            Part test2 = ctx.getPart("test2");
            try (InputStream input1 = test1.getInputStream();
                 InputStream input2 = test2.getInputStream()) {
                String value = $.io.toString(input1);
                System.out.println(value);

                String value2 = $.io.toString(input2);
                System.out.println(value2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received multi part data");
        }).router().post("/upload/poetry").handler(ctx -> {
            // upload poetry
            Part poetry = ctx.getPart("poetry");
            System.out.println(poetry.getSubmittedFileName());
            try (InputStream inputStream = $.class.getResourceAsStream("/poem.txt")) {
                String poem = $.io.toString(inputStream);
                System.out.println(poem);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received poetry");
        }).listen(host, port);

        $.httpClient().post(uri + "/upload/string")
         .addFieldPart("test1", new StringContentProvider("hello multi part1"), null)
         .addFieldPart("test2", new StringContentProvider("hello multi part2"), null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        InputStream inputStream = $.class.getResourceAsStream("/poem.txt");
        InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(inputStream);
        $.httpClient().post(uri + "/upload/poetry")
         .addFilePart("poetry", "poem.txt", inputStreamContentProvider, null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             $.io.close(inputStreamContentProvider);
             $.io.close(inputStream);
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}
```

The HTTP server uses the **ctx.getPart** to get the content of multi-part format, and the client uses the **httpclient.addFilePart** to upload content using multi-part format.
