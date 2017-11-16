---

category : docs
layout: document
title: HTTP server and client Kotlin extension

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Introduction](#introduction)
- [The first example](#the-first-example)
- [Route order](#route-order)
- [Set the handler listener](#set-the-handler-listener)
- [Capturing path parameters](#capturing-path-parameters)
- [Routing by wildcard](#routing-by-wildcard)
- [Routing by regular expressions](#routing-by-regular-expressions)
- [Routing by HTTP method](#routing-by-http-method)
- [Routing based on MIME type of request](#routing-based-on-mime-type-of-request)
- [Routing based on MIME types acceptable by the client](#routing-based-on-mime-types-acceptable-by-the-client)

<!-- /TOC -->

# Introduction
Kotlin is a graceful modern programming language. It is a great fit for developing server-side applications. Firefly Java APIs is fully compatible with Kotlin. That means you can use the Firefly framework in the Kotlin directly. But the Kotlin provides coroutine, reified type parameter, type-safe builder and many other powerful features. The coroutine simplifies building the asynchronous applications. It makes the programs are shorter and far simpler to understand and enjoy the scalability and performance benefits of asynchronous codes. The reified type parameter and type-safe builder reduces the amount of boilerplate code and helps up to build powerful and easy-to-use abstractions. So we decide to use these powerful features to build new Firefly Kotlin DSL APIs.

# The first example
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/hello"

            asyncHandler {
                end("hello world")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/hello").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```

Run it. The console shows:
```
200
hello world
```

In this example, we demonstrate how to use the Kotlin DSL to build an HTTP server. You can write the business logic in the `asyncHandler` block. It runs on the coroutines.

The HTTP client uses the `asyncSubmit` function to wait for the response. This function runs on the coroutine. It can avoid "callback hell" effectively. It is similar to the synchronous blocking code style. But the `asyncSubmit` function does not block current thread. It only suspends the current function. More details, you can see the [Kotlin coroutine document](http://kotlinlang.org/docs/reference/coroutines.html).

# Route order
By default, routes are matched in the order which they are added to the router manager. But you can also specify a router ID to decide the route order.
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/routeOrder"

            asyncHandler {
                write("The first router.\r\n").next()
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/routeOrder"

            asyncHandler {
                end("The last router.")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/routeOrder").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```
Run it. The console shows:
```
200
The first router.
The last router.
```

Specify a router ID to decide the route order:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router(101) {
            httpMethod = HttpMethod.GET
            path = "/routeOrder"

            asyncHandler {
                end("Into router ${getId()}.")
            }
        }

        router(100) {
            httpMethod = HttpMethod.GET
            path = "/routeOrder"

            asyncHandler {
                write("Into router ${getId()}.\r\n").next()
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/routeOrder").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```
Run it. The console shows:
```
200
Into router 100.
Into router 101.
```

# Set the handler listener
The Kotlin HTTP server execute handler asynchronously. If you want to listen the next handler complete event, you need use `asyncNext` function and set a listener. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/handlerListener"

            asyncHandler {
                write("Into the first handler.\r\n")
                asyncNext<String> {
                    write(it)
                    end("Complete the first handler.")
                }
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/handlerListener"

            asyncHandler {
                write("Into the second handler.\r\n")
                asyncNext<String> {
                    write(it)
                    asyncSucceed("Complete the second handler.\r\n")
                }
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/handlerListener"

            asyncHandler {
                write("Into the last handler.\r\n")
                asyncSucceed("Complete the last handler.\r\n")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/handlerListener").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```

Run it. The console shows:
```
200
Into the first handler.
Into the second handler.
Into the last handler.
Complete the last handler.
Complete the second handler.
Complete the first handler.
```

The `asyncNext` function receives two parameters:
- _**succeeded**_ - The handler complete listener. You can call the `asyncSucceed` function to fire this listener.
- _**failed**_ - The handler exception listener. By the default, the exception event is ignored. If you want to listen the exception event, when the handler throws an exception, you need catch it and call the `asyncFail` function to fire this listener.

# Capturing path parameters
It’s possible to match paths using placeholders for parameters. The placeholders consist of ":" followed by the parameter name. Parameter names consist of any alphabetic character, numeric character or underscore. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Get the product $id")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/product/20").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```
Run it. The console shows:
```
200
Get the product 20
```

# Routing by wildcard
Often you want to route all requests that accord with a pattern. You could use a regex to do this, but a simply way is to use an asterisk wildcard `*`. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/product*"

            asyncHandler {
                val matched = getWildcardMatchedResult(0)
                write("Intercept the product: $matched\r\n")
                asyncNext<String> {
                    end(it)
                }
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/product/:type"

            asyncHandler {
                val type = getPathParameter("type")
                write("List $type\r\n")
                asyncSucceed("List $type success")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/product/apple").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```
Run it. The console shows:
```
200
Intercept the product: /apple
List apple
List apple success
```
We use the `getWildcardMatchedResult` function to get the matched part and the index starts from 0.

# Routing by regular expressions
Regular expressions can also be used to match URI paths in routes. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            regexPath = "/product(.*)"

            asyncHandler {
                val matched = getRegexGroup(1)
                write("Intercept the product: $matched\r\n")
                asyncNext<String> {
                    end(it)
                }
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/product/:type"

            asyncHandler {
                val type = getPathParameter("type")
                write("List $type\r\n")
                asyncSucceed("List $type success")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/product/orange").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}
```
Run it. The console shows:
```
200
Intercept the product: /orange
List orange
List orange success
```
We use the `getRegexGroup` function to get the matched group and the index starts from 1.

# Routing by HTTP method
We can set value for the `httpMethod` property to match the HTTP request. If you don't set any value to the `httpMethod` property, the router will match all HTTP methods. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = GET
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Get the product $id.")
            }
        }

        router {
            httpMethod = POST
            path = "/product"

            asyncHandler {
                end("Create a new product: $stringBody")
            }
        }

        router {
            httpMethod = PUT
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Update the product $id: $stringBody")
            }
        }

        router {
            httpMethod = DELETE
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Delete the product $id")
            }
        }
    }.listen(host, port)

    val getResp = firefly.httpClient().get("http://$host:$port/product/20").asyncSubmit()
    println(getResp.stringBody)

    val postResp = firefly.httpClient().post("http://$host:$port/product")
            .body("Car 20. The color is red.").asyncSubmit()
    println(postResp.stringBody)

    val putResp = firefly.httpClient().put("http://$host:$port/product/20")
            .body("Change the color from red to black.").asyncSubmit()
    println(putResp.stringBody)

    val delResp = firefly.httpClient().delete("http://$host:$port/product/20").asyncSubmit()
    println(delResp.stringBody)
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

If you want to let a lot of HTTP methods match a router, just use the `httpMethods` property instead of `httpMethod`. Its type is `List`.

# Routing based on MIME type of request
You can specify that a route will match against matching request MIME types using `consumes` property.

In this case, the request will contain a content-type header specifying the MIME type of the request body. This will be matched against the value specified in consumes.

Basically, the `consumes` is describing which MIME types the handler can consume. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.PUT
            path = "/product/:id"
            consumes = "*/json"

            asyncHandler {
                val id = getPathParameter("id")
                val type = getWildcardMatchedResult(0)
                val car = getJsonBody<Car>()
                end("Update resource $id: $car. The content type is $type/json")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().put("http://$host:$port/product/20")
            .jsonBody(Car(20, "My car", "black")).asyncSubmit()
    println(resp.stringBody)
}

@NoArg
data class Car(var id: Long,
               var name: String,
               var color: String)
```
In the above example, we use the wildcard `*` to match the content type of the HTTP request. We can also use the exact MIME type to match the request.  

The server uses the `getJsonBody<Car>()` function to get JSON data. This function receives a reified type parameter `Car`. The JSON parser will bind the JSON data to that type.

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

By using `produces` property you define which MIME type(s) the route produces, e.g. the following handler produces a response with MIME type application/json. For example:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.PUT
            path = "/product/:id"
            consumes = "*/json"
            produces = "text/plain"

            asyncHandler {
                val id = getPathParameter("id")
                val car = getJsonBody<Car>()
                end("Update resource $id: $car")
            }
        }

        router {
            httpMethod = HttpMethod.PUT
            path = "/product/:id"
            consumes = "*/json"
            produces = "application/json"

            asyncHandler {
                writeJson(getJsonBody<Car>()).end()
            }
        }
    }.listen(host, port)

    val text = firefly.httpClient().put("http://$host:$port/product/20")
            .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
            .jsonBody(Car(20, "My car", "black"))
            .asyncSubmit()
    println(text.stringBody)

    val json = firefly.httpClient().put("http://$host:$port/product/20")
            .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
            .jsonBody(Car(20, "My car", "black"))
            .asyncSubmit()
    println(json.stringBody)
}
```
Run it. The console shows:
```
Update resource 20: Car(id=20, name=My car, color=black)
{"color":"black","id":20,"name":"My car"}
```
In the above example, the first request, the `text/plain` weight(1.0) is higher than `application/json`(0.9), so this request matches the first router that responds the text format.   

The second request, the `application/json` weight equals the `text/plain`, but `application/json` is in front of `text/plain`, so the `application/json` priority is higher than `text/plain`. It matches the second router that responds the JSON format.
