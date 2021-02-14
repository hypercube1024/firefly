---

category : docs
title: HTTP server and client

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
  - [Class $](#class-)
  - [Route order](#route-order)
  - [Calling the next handler](#calling-the-next-handler)
  - [Routing Context](#routing-context)
- [Capturing path parameters](#capturing-path-parameters)
- [Routing by wildcard](#routing-by-wildcard)
- [Routing by regular expressions](#routing-by-regular-expressions)
- [Routing by HTTP method](#routing-by-http-method)
- [Routing based on MIME type of request](#routing-based-on-mime-type-of-request)
- [Routing based on MIME types acceptable by the client](#routing-based-on-mime-types-acceptable-by-the-client)
- [Error handling](#error-handling)
  - [Custom error handling](#custom-error-handling)
- [Serving static resources](#serving-static-resources)
- [Multipart file uploading](#multipart-file-uploading)
- [CORS handler](#cors-handler)

<!-- /TOC -->

# Basic concepts
The router is one of the core concepts of Firefly HTTP server. It maintains zero or more Routes.

A router takes an HTTP request and finds the first matching route for that request, and passes the request to that route.

The route can have a handler associated with it, which then receives the request. You then do something with the request, and then, either end it or pass it to the next matching handler.

Here’s a simple router example:
```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx -> ctx.write("Hello world! ").next() } // (1)
        .router().get("/").handler { ctx -> ctx.end("The router demo.") } // (2)
        .listen("localhost", 8090)
}
```
Two routers handle GET method and path '/'. When you visit the `http://localhost:8090`, the server response:
```
Hello world! The router demo.
```

## Class $
The class $ provides primary API of Firefly, such as
- The HTTP server and client
- The WebSocket server and client
- The TCP server and client
- The UDP server and client
- Other utilities

It uses fluent style API to help you to build a complex application. Chaining calls like this allow you to write code that’s a little bit less verbose.

## Route order
By default, routes are matched in the order which they are added to the router manager. Invokes the method `$.httpServer().router()` to create a router.  

When a request arrives, the router will step through each route and check if it matches then the handler for that route will be called.

## Calling the next handler
If the handler subsequently calls `ctx.next()` method the handler for the next matching route (if any) will be called. And so on.

In the first example, When the server receives the request, it invokes the first handler. 
1. The first handler writes the "Hello world!" and calls `ctx.next()` method to invoke the next router.
2. The second handler writes the "The router demo." and ends the chain of responsibility. Then the server will flush the data to the client. 

## Routing Context
A new RoutingContext(ctx) instance is created for each HTTP request.

You can visit the RoutingContext instance in the whole router chain. It provides HTTP request/response API and you can save data in the routing context.   

Here’s an example where one handler saves data in the routing context, and a subsequent handler reads it:
```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx ->
            ctx.attributes["router1"] = "Some one visits the /. " // (1)
            ctx.write("Hello world! ").next()
        }
        .router().get("/").handler { ctx ->
            val data = ctx.attributes["router1"] // (2)
            ctx.end("The router data: $data")
        }
        .listen("localhost", 8090)
}
```
You visit the `http://localhost:8090` , the server response:
```
Hello world! The router data: Some one visits the /.
```
1. The first handler saves data using `ctx.attributes` method.
2. The second handler reads data and flushes it to the client.


# Capturing path parameters
It’s possible to match paths using placeholders for parameters. The placeholders consist of : followed by the parameter name. Parameter names consist of any alphabetic character, numeric character or underscore.

```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/product/:id").handler { ctx ->
            when (val id = ctx.getPathParameter("id")) {
                "1" -> ctx.end("Apple")
                "2" -> ctx.end("Orange")
                else -> ctx.setStatus(NOT_FOUND_404).end("The product $id not found.")
            }
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().get("$url/product/1").submit()
        .thenAccept { response -> println(response.stringBody) } // (1)

    `$`.httpClient().get("$url/product/2").submit()
        .thenAccept { response -> println(response.stringBody) } // (2)

    `$`.httpClient().get("$url/product/3").submit()
        .thenAccept { response -> println(response.stringBody) } // (3)
}
```
In this case, we use the HTTP client to call the HTTP server.  
1. The `$.httpClient().get` builds the GET request and then submits it to the server, the server uses `ctx.getPathParameter(name: String)` method to get product id in the path. The server finds the apple and response `Apple`.
2. The server gets the orange and respone `Orange`
3. The server can not find the product 3, so response `The product 3 not found.`


# Routing by wildcard
Often you want to route all requests that accord with a pattern. A simply way is to use wildcard `*`. For example:
```kotlin
fun main() {
    `$`.httpServer()
        .router().put("/product/*/*").handler { ctx ->
            val type = ctx.getPathParameter(0)
            val id = ctx.getPathParameter(1)
            val product = ctx.stringBody
            ctx.end("Put product success. id: $id, type: $type, product: $product")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().put("$url/product/fruit/1").body("Apple").submit()
        .thenAccept { response -> println(response.stringBody) } // (1)

    `$`.httpClient().put("$url/product/book/1").body("Tom and Jerry").submit()
        .thenAccept { response -> println(response.stringBody) } // (2)

    `$`.httpClient().put("$url/product/book/2").body("The Three-Body Problem").submit()
        .thenAccept { response -> println(response.stringBody) } // (3)
}
```
The server uses `ctx.getPathParameter(index: Int)` method to get path parameter. The index starts from 0.
1. The `$.httpClient().put` builds the PUT request and then submits it to the server. The server response `Put product success. id: 1, type: fruit, product: Apple`.
2. The server response `Put product success. id: 1, type: book, product: Tom and Jerry`.
3. The server response `Put product success. id: 2, type: book, product: The Three-Body Problem`.


# Routing by regular expressions
Regular expressions can also be used to match URI paths in routes. For example:
```kotlin
fun main() {
    `$`.httpServer()
        .router().method(HttpMethod.PUT).pathRegex("/product/(.*)/(.*)").handler { ctx ->
            val type = ctx.getPathParameterByRegexGroup(1)
            val id = ctx.getPathParameterByRegexGroup(2)
            val product = ctx.stringBody
            ctx.end("Put product success. id: $id, type: $type, product: $product")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().put("$url/product/fruit/1").body("Apple").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().put("$url/product/book/1").body("Tom and Jerry").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().put("$url/product/book/2").body("The Three-Body Problem").submit()
        .thenAccept { response -> println(response.stringBody) }
}
```

We use the `getPathParameterByRegexGroup` method to get the path parameter. The regex group index starts from 1.


# Routing by HTTP method
By the default, a route will match all HTTP methods if you do not call `router.method`, `router.get`, `router.post`, `router.put` or `router.delete`. For example:
```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Get the product $id")
        }
        .router().post("/product").handler { ctx ->
            ctx.end("Create the product 1")
        }
        .router().put("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Update the product $id")
        }
        .router().delete("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Delete the product $id")
        }
        .listen("localhost", 8090)
}
```
In the above example, we build the RESTful APIs. The URL `/product/:id` represents resources. The HTTP verbs (Such as, `GET`, `POST`, `PUT`, `DELETE` and so on) represent the operation of resources (Such as get, create, update and delete).  

If you want to let a lot of HTTP methods match a router, just use the `router.methods(list: List)` instead of `router.method(name: String)`.

# Routing based on MIME type of request
You can specify that a route will match against matching request MIME types using method `router.consumes`.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, The `router.consumes` method is describing which MIME types the handler can consume. For example:
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

# CORS handler
Cross Origin Resource Sharing is a safe mechanism for allowing resources to be requested from one domain and served from another.

The example:
```java
CORSConfiguration config = new CORSConfiguration();
config.setAllowOrigins(new HashSet<>(Arrays.asList("http://foo.com", "http://bar.com")));
config.setExposeHeaders(Arrays.asList("a1", "a2"));
config.setAllowHeaders(new HashSet<>(Arrays.asList("a1", "a2", "a3", "a4")));
CORSHandler corsHandler = new CORSHandler();
corsHandler.setConfiguration(config);

HTTP2ServerBuilder s = $.httpServer();
SimpleHTTPClient c = $.createHTTPClient();

s.router().path("/cors/*").handler(corsHandler)
 .router().path("/cors/foo").handler(ctx -> ctx.end("foo"))
 .router().path("/cors/bar").handler(ctx -> {
    JsonObject jsonObject = ctx.getJsonObjectBody();
    Map<String, Object> map = new HashMap<>(jsonObject);
    map.put("bar", "x1");
    ctx.writeJson(map).end();
})
 .listen(host, port);

SimpleResponse resp = c.get(uri + "/cors/foo")
                       .put(HttpHeader.ORIGIN, "http://foo.com")
                       .put(HttpHeader.HOST, "foo.com")
                       .submit().get(2, TimeUnit.SECONDS);
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN), is("http://foo.com"));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS), is("a1, a2"));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));

resp = c.request(HttpMethod.OPTIONS, uri + "/cors/bar")
        .put(HttpHeader.ORIGIN, "http://bar.com")
        .put(HttpHeader.HOST, "bar.com")
        .put(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET, POST, PUT, DELETE")
        .put(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, "a2, a3, a4")
        .put("a2", "foo_a2")
        .submit().get(2, TimeUnit.SECONDS);
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN), is("http://bar.com"));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));
System.out.println(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS));
System.out.println(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).contains("DELETE"), is(true));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).contains("a2"), is(true));
Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_MAX_AGE), is("86400"));

c.stop();
s.stop();
```
