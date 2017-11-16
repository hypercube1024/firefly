---

category : docs
layout: document
title: HTTP server and client

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
	- [Class $](#class-)
	- [Route order](#route-order)
	- [Calling the next handler](#calling-the-next-handler)
	- [Routing Context](#routing-context)
- [Capturing path parameters](#capturing-path-parameters)
- [Routing by exact path](#routing-by-exact-path)
- [Routing by wildcard](#routing-by-wildcard)
- [Routing by regular expressions](#routing-by-regular-expressions)
- [Routing by HTTP method](#routing-by-http-method)
- [Routing based on MIME type of request](#routing-based-on-mime-type-of-request)
- [Routing based on MIME types acceptable by the client](#routing-based-on-mime-types-acceptable-by-the-client)
- [Error handling](#error-handling)
	- [Custom error handling](#custom-error-handling)
- [Handling sessions](#handling-sessions)
	- [Local session store](#local-session-store)
	- [Remote session store](#remote-session-store)
- [Serving static resources](#serving-static-resources)
- [Rendering template](#rendering-template)
	- [Custom template renderer](#custom-template-renderer)
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
By default, routes are matched in the order which they are added to the router manager. The method **httpServer.router()** creates a router and adds it to the router manager.
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

Here’s an example where one handler sets some data in the context data, and a subsequent handler retrieves it:

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
        String host = "localhost";
        int port = 8081;

        $.httpServer().router().get("/product/:id")
         .handler(ctx -> {
             String id = ctx.getRouterParameter("id");
             ctx.end($.string.replace("Get the product {}", id));
         }).listen(host, port);

        $.httpClient()
         .get($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
```
Run it. The console shows:
```
200
Get the product 20
```

# Routing by exact path
A route can be set-up to match the path from the request URI. In this case, it will match any request which has a path that’s the same as the specified path.

In the following example, the handler will be called for a request /product/tools/. We also ignore trailing slashes so that it will be called for paths /product/tools and /product/tools/ too:
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

# Routing by wildcard
Often you want to route all requests that accord with a pattern. You could use a regex to do this, but a simply way is to use an asterisk wildcard `*`. For example:
```java
public class RoutingByPathsWithWildcardDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().get("/product*")
         .handler(ctx -> {
             String matched = ctx.getWildcardMatchedResult(0);
             ctx.write("Intercept the product: " + matched + "\r\n").next();
         })
         .router().get("/product/:type")
         .handler(ctx -> {
             String type = ctx.getPathParameter("type");
             ctx.end("List " + type + "\r\n");
         })
         .listen(host, port);

        $.httpClient().get($.string.replace("http://{}:{}/product/apple", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
```
Run it. The console shows:
```
200
Intercept the product: /apple
List apple
```
We use the `getWildcardMatchedResult` function to get the matched part and the index starts from 0.

# Routing by regular expressions
Regular expressions can also be used to match URI paths in routes. For example:
```java
public class RoutingWithRegexDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().method(HttpMethod.GET).pathRegex("/product(.*)")
         .handler(ctx -> {
             String matched = ctx.getRegexGroup(1);
             ctx.write("Intercept the product: " + matched + "\r\n").next();
         })
         .router().get("/product/:type")
         .handler(ctx -> {
             String type = ctx.getPathParameter("type");
             ctx.end("List " + type + "\r\n");
         })
         .listen(host, port);

        $.httpClient().get($.string.replace("http://{}:{}/product/orange", host, port))
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStatus());
             System.out.println(resp.getStringBody());
         });
    }
}
```
Run it. The console shows:
```
200
Intercept the product: /orange
List orange
```
We use the `getRegexGroup` function to get the matched group and the index starts from 1.


# Routing by HTTP method
By the default, a route will match all HTTP methods if you do not call **router.method**, **router.get**, **router.post**, **router.put** or **router.delete**. For example:
```java
public class RoutingByMethods {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().get("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             ctx.end($.string.replace("Get the product {}", id));
         })
         .router().post("/product")
         .handler(ctx -> {
             String product = ctx.getStringBody();
             ctx.end($.string.replace("Create a new product: {}", product));
         })
         .router().put("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String product = ctx.getStringBody();
             ctx.end($.string.replace("Update the product {}: {}", id, product));
         })
         .router().delete("/product/:id")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             ctx.end($.string.replace("Delete the product {}", id));
         })
         .listen(host, port);

        $.httpClient()
         .get($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .post($.string.replace("http://{}:{}/product", host, port))
         .body("Car 20. The color is red.")
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .put($.string.replace("http://{}:{}/product/20", host, port))
         .body("Change the color from red to black.")
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient()
         .delete($.string.replace("http://{}:{}/product/20", host, port))
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
```
Run it. The console shows:
```
Get the product 20.
Create a new product: Car 20. The color is red.
Update the product 20: Change the color from red to black.
Delete the product 20
```
In the above example, we build the RESTful APIs. The URL `/product/:id` represents resources. The HTTP verbs (Such as, `GET`, `POST`, `PUT`, `DELETE` and so on) represent the operation of resources (Such as get, create, update and delete).  

If you want to let a lot of HTTP methods match a router, just use the **router.methods** instead of **router.method**. Its type is `List`.

# Routing based on MIME type of request
You can specify that a route will match against matching request MIME types using **router.consumes**.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, **router.consumes** is describing which MIME types the handler can consume. For example:
```java
public class RoutingByConsumes {

    public static class Car {
        public Long id;
        public String name;
        public String color;

        @Override
        public String toString() {
            return "Car{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", color='" + color + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().put("/product/:id").consumes("*/json")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String type = ctx.getWildcardMatchedResult(0);
             Car car = ctx.getJsonBody(Car.class);
             ctx.end($.string.replace("Update resource {}: {}. The content type is {}/json", id, car, type));
         })
         .listen(host, port);

        Car car = new Car();
        car.id = 20L;
        car.name = "My car";
        car.color = "black";

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
```
Run it. The console shows:
```
Update resource 20: Car{id=20, name='My car', color='black'}. The content type is application/json
```
In the above example, we use the wildcard `*` to match the content type of the HTTP request. We can also use the exact MIME type to match the request.  

# Routing based on MIME types acceptable by the client
The HTTP Accept header is used to signify which MIME types of the response are acceptable to the client.

An accept header can have multiple MIME types separated by ‘,’.

MIME types can also have a q value appended to them which signifies a weighting to apply if more than one response MIME type is available matching the HTTP Accept header. The q value is a number between 0 and 1.0. If omitted it defaults to 1.0.

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

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().put("/product/:id").consumes("*/json").produces("text/plain")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String type = ctx.getWildcardMatchedResult(0);
             Car car = ctx.getJsonBody(Car.class);
             ctx.end($.string.replace("Update resource {}: {}. The content type is {}/json", id, car, type));
         })
         .router().put("/product/:id").consumes("*/json").produces("application/json")
         .handler(ctx -> {
             Car car = ctx.getJsonBody(Car.class);
             ctx.writeJson(car).end();
         })
         .listen(host, port);

        Car car = new Car();
        car.id = 20L;
        car.name = "My car";
        car.color = "black";

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
```
Run it. The console shows:
```
Update resource 20: Car{id=20, name='My car', color='black'}. The content type is application/json
{"color":"black","id":20,"name":"My car"}
```
In the above example, the first request, the `text/plain` weight(1.0) is higher than `application/json`(0.9), so this request matches the first router that responds the text format.   

The second request, the `application/json` weight equals the `text/plain`, but `application/json` is in front of `text/plain`, so the `application/json` priority is higher than `text/plain`. It matches the second router that responds the JSON format.

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

powered by Firefly {{ site.data.global.releaseVersion }}
```

## Custom error handling
Also you can extend **com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler** and add the SPI configuration instead of the default error handler.

For example.
```java
public class DefaultErrorResponseHandler extends AbstractErrorResponseHandler {

    @Override
    public void render(RoutingContext ctx, int status, Throwable t) {
        HttpStatus.Code code = HttpStatus.getCode(status);
        String title = status + " " + (code != null ? code.getMessage() : "error");
        String content;
        switch (code) {
            case NOT_FOUND: {
                content = "Custom error handler. The resource " + ctx.getURI().getPath() + " is not found";
            }
            break;
            case INTERNAL_SERVER_ERROR: {
                content = "Custom error handler. The server internal error. <br/>" + (t != null ? t.getMessage() : "");
            }
            break;
            default: {
                content = title + "<br/>" + (t != null ? t.getMessage() : "");
            }
            break;
        }
        ctx.setStatus(status).put(HttpHeader.CONTENT_TYPE, "text/html")
           .write("<!DOCTYPE html>")
           .write("<html>")
           .write("<head>")
           .write("<title>")
           .write(title)
           .write("</title>")
           .write("</head>")
           .write("<body>")
           .write("<h1> " + title + " </h1>")
           .write("<p>" + content + "</p>")
           .write("<hr/>")
           .write("<footer><em>powered by Firefly " + Version.value + "</em></footer>")
           .write("</body>")
           .end("</html>");
    }
}
```

The SPI configuration file **com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler** at **${classpath}/META-INF/services**.
```
com.firefly.example.error.handler.DefaultErrorResponseHandler
```

Visit "http://localhost:8080/error" and view:

```
500 Server Error

Custom error handler. The server internal error.
perhaps some errors happen

powered by Firefly {{ site.data.global.releaseVersion }}
```



# Handling sessions
Firefly uses session cookies to identify a session. The session cookie is temporary and will be deleted by your browser when it’s closed.

We don’t put the actual data of your session in the session cookie - the cookie simply uses an identifier to look-up the actual session on the server. The identifier is a random UUID generated using a secure random, so it should be effectively unguessable.

Cookies are passed across the wire in HTTP requests and responses, so we recommend you to enable HTTPS when sessions are being used.

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

Run it and view.
```
http://localhost:8080/example
```
The result.
```
Name: Item 1
Price: $19.99
    Feature: New!
    Feature: Awesome!
Name: Item 2
Price: $29.99
    Feature: Old.
    Feature: Ugly.
```

## Custom template renderer
You can use the other template engine instead of the Mustache. The first, implement the TemplateHandlerSPI interface.
```java
public class TemplateSPIImpl implements TemplateHandlerSPI {
    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, Object scope) {
        ctx.end("test template spi demo");
    }

    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, Object[] scopes) {
        ctx.end("test template spi demo");
    }

    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, List<Object> scopes) {
        ctx.end("test template spi demo");
    }
}
```

Create an new file **com.firefly.server.http2.router.spi.TemplateHandlerSPI** at **${classpath}/META-INF/services** and add the class name in this file.
```
com.firefly.example.template.spi.TemplateSPIImpl
```

Create main method.
```java
public static void main(String[] args) {
		$.httpServer().router().get("/example").handler(ctx -> {
				ctx.put(HttpHeader.CONTENT_TYPE, "text/plain")
					 .renderTemplate("template/example.mustache", new Example());
		}).listen("localhost", 8080);
}
```
Run it and view.
```
http://localhost:8080/example
```

The result.
```
test template spi demo
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
