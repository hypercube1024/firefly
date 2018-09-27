package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import io.swagger.models.*
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.StringProperty

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
                end(writePrettyJson(openApiV2()))
            }
        }
    }.listen(host, port)
}

fun openApiV2(): Swagger {
    val address = ModelImpl()
    address.properties = mapOf(
        "street" to StringProperty().example("Jie Dao Kou").description("the street number"),
        "city" to StringProperty().example("Wuhan").description("city"),
        "state" to StringProperty().example("Hubei").description("state"),
        "zip" to StringProperty()
            .minLength(2).maxLength(2)
            .pattern("^\\d{5}(?:[-\\s]\\d{4})?\$")
            .example("333")
            .description("zip"),
        "country" to StringProperty()
            .minLength(2).maxLength(2)
            ._enum(listOf("US")).example("US").description("country")
                              )
    address.example = Address(
        "test street",
        "test city",
        "test state",
        "333",
        "US"
                             )

    val s = Swagger()
        .info(
            Info()
                .title("test open api v2").version("v2")
                .contact(
                    Contact()
                        .email("hello@v2.com")
                        .url("http://www.hellov2.com")
                        )
             )
        .scheme(Scheme.HTTP)
        .host("www.hello.com")
        .basePath("/testV2")
        .consumes(listOf("application/json"))
        .produces(listOf("application/json"))
        .paths(
            mapOf(
                "/address" to Path().get(
                    Operation().summary("get address")
                        .parameter(
                            QueryParameter()
                                .name("id")
                                .description("The address id")
                                .required(false)
                                .type("integer")
                                .format("int64")
                                .example("10")
                                  )
                        .response(
                            200, Response().description("Address info")
                                .responseSchema(RefModel("#/definitions/Address"))
                                .example("application/json", address.example)
                                 )
                                        )
                 )
              )

    s.addDefinition("Address", address)
    return s
}