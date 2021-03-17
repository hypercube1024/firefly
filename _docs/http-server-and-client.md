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
- [Routing by content type](#routing-by-content-type)
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

# Routing by content type
You can specify that a route will match against matching request MIME types using method `router.consumes`.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, The `router.consumes` method is describing which MIME types the handler can consume. For example:
```kotlin
@NoArg
data class Car(var name: String, var color: String)

fun main() {
    `$`.httpServer()
        .router().put("/product/:id").consumes("*/json")
        .handler { ctx ->
            val id = ctx.getPathParameter("id")
            val type = ctx.getPathParameter(0)
            val car = json.read<Car>(ctx.stringBody)

            ctx.write("Update product. id: $id, type: $type. \r\n")
                .end(car.toString())
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient()
        .put("$url/product/3")
        .add(HttpField(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value))
        .body(json.write(Car("Benz", "Black")))
        .submit().thenAccept { response -> println(response.stringBody) }
}
```
Run it. The console shows:
```
Update product. id: 3, type: application. 
Car(name=Benz, color=Black)
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

By using `router.produces` you define which MIME type(s) the route produces, e.g. the following handler produces a response with MIME type application/json.

```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/product/:id").produces("text/plain")
        .handler { ctx ->
            ctx.end(Car("Benz", "Black").toString())
        }
        .router().get("/product/:id").produces("application/json")
        .handler { ctx ->
            ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                .end(json.write(Car("Benz", "Black")))
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().get("$url/product/3")
        .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
        .submit().thenAccept { response -> println("accept text; ${response.stringBody}") }

    `$`.httpClient().get("$url/product/3")
        .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
        .submit().thenAccept { response -> println("accept json; ${response.stringBody}") }
}
```
Run it. The console shows:
```
accept text; Car(name=Benz, color=Black)
accept json; {"name":"Benz","color":"Black"}
```
In the above example, the first request, the `text/plain` weight(1.0) is higher than `application/json`(0.9), so this request matches the first router that responds the text format.   

The second request, the `application/json` weight equals the `text/plain`, but `application/json` is in front of `text/plain`, so the `application/json` priority is higher than `text/plain`. It matches the second router that responds the JSON format.

# Error handling
When the handler throws exception, the server uses `DefaultContentProvider` response the error message.

```kotlin
fun main() {
    `$`.httpServer()
        .router().post("/product").handler {
            throw IllegalStateException("Create product exception") // (1)
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/").submit()
        .thenAccept { response -> println(response) }
}
```
The client will receive the `Create product exception`.

## Custom error handling
You can use `httpServer.onException` method to output custom error message.

For example.
```kotlin
fun main() {
    `$`.httpServer()
        .onException { ctx, exception -> // (1)
            ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .end("The server exception. ${exception.message}")
        }
        .router().post("/product").handler {
            throw IllegalStateException("Create product exception")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/").submit()
        .thenAccept { response -> println(response) }
}
```
The client will receive the `The server exception. Create product exception`.


# Serving static resources
Firefly comes with an out of the box handler for serving static web resources so you can write static web servers very easily.

To serve static resources such as .html, .css, .js or any other static resource, you use an instance of `FileHandler`.
```kotlin
fun main() {
    `$`.httpServer()
        .router().method(HttpMethod.GET)
        .paths(listOf("/favicon.ico", "/poem.html", "/poem.txt")) // (1)
        .handler(FileHandler.createFileHandlerByResourcePath("files")) // (2)
        .listen("localhost", 8090)
}
```
1. Use the `router.paths` method to bind some path to the file handler.
2. Use the factory method `FileHandler.createFileHandlerByResourcePath` to create a file handler instance. When you visit the `http://localhost:8090/poem.html`, the server will read the file `resources/files/poem.html` and flush the file to the client.

# Multipart file uploading

```kotlin
@NoArg
data class Product(var id: String, var brand: String, var description: String)

fun main() {
    `$`.httpServer()
        .router().post("/product/file-upload").handler { ctx ->
            val id = ctx.getPart("id") // (1)
            val brand = ctx.getPart("brand")
            val description = ctx.getPart("description")
            ctx.end(Product(id.stringBody, brand.stringBody, description.stringBody).toString())
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/file-upload")
        .addPart("id", stringBody("x01"), null) // (2)
        .addPart("brand", stringBody("Test"), null)
        .addFilePart(
            "description", "poem.txt",
            resourceFileBody("files/poem.txt", StandardOpenOption.READ), // (3)
            null
        )
        .submit().thenAccept { response -> println(response) }
}
```

1. The server uses the `ctx.getPart` to get the content of multi-part format.
2. The client uses the `httpclient.addPart` to upload content.
3. The client uses the `httpclient.addFilePart` to upload file. The factory method `resourceFileBody` reads the resource file and encodes it to the multi-part format.

# CORS handler
Cross Origin Resource Sharing is a safe mechanism for allowing resources to be requested from one domain and served from another.

The example:
```kotlin
fun main() {
    val corsConfig = CorsConfig("*.cors.test.com") 
    `$`.httpServer()
        .router().path("*").handler(CorsHandler(corsConfig)) // (1)
        .router().post("/cors-data-request/*")
        .handler { it.end("success") }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/cors-data-request/xxx")
        .put(HttpHeader.ORIGIN, "hello.cors.test.com") // (2)
        .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN_UTF_8.value)
        .body("hello")
        .submit().thenAccept { response -> println(response) }
}
```
1. The server uses the `CorsHandler` to set some origin can visit the server resources.
2. The client set the origin header, if the server allows this origin, the client can visit the server resources. 
