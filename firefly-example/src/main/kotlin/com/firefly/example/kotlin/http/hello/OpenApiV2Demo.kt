package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import io.swagger.models.Contact
import io.swagger.models.Info
import io.swagger.models.Scheme
import io.swagger.models.Swagger

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/openApiV2.json"

            asyncHandler {
                end("")
            }
        }
    }.listen(host, port)
}

fun openApiV2(): Swagger {
    val s = Swagger()
            .info(Info()
                    .title("test open api v2").version("v2")
                    .contact(Contact()
                            .email("hello@v2.com")
                            .url("http://www.hellov2.com")))
            .scheme(Scheme.HTTP)
            .host("http://www.hellov2.com/test")
            .basePath("s2")
    return s
}