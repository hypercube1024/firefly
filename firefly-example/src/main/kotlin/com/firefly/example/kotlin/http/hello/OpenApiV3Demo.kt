package com.firefly.example.kotlin.http.hello

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.http.HttpServer
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.links.Link
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.tags.Tag
import java.math.BigDecimal
import java.util.*

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/openApiV3.json"

            asyncHandler {
                end(writePrettyJson(openApi()))
            }
        }
    }.listen(host, port)
}

fun openApi(): OpenAPI {
    val oai = OpenAPI()
            .info(Info()
                    .contact(Contact()
                            .email("tony@eatbacon.org")
                            .name("Tony the Tam")
                            .url("https://foo.bar")))
            .externalDocs(ExternalDocumentation()
                    .description("read more here")
                    .url("http://swagger.io"))
            .addTagsItem(Tag()
                    .name("funky dunky")
                    .description("all about neat things"))
            .extensions(object : HashMap<String, Any>() {
                init {
                    put("x-fancy-extension", "something")
                }
            })

    val schemas = HashMap<String, Schema<*>>()

    schemas["StringSchema"] = StringSchema()
            .description("simple string schema")
            .minLength(3)
            .maxLength(100)
            .example("it works")

    schemas["IntegerSchema"] = IntegerSchema()
            .description("simple integer schema")
            .multipleOf(BigDecimal(3))
            .minimum(BigDecimal(6))

    oai.components(Components().schemas(schemas))

    schemas["Address"] = Schema<Address>()
            .description("address object")
            .addProperties("street", StringSchema()
                    .description("the street number"))
            .addProperties("city", StringSchema()
                    .description("city"))
            .addProperties("state", StringSchema()
                    .description("state")
                    .minLength(2)
                    .maxLength(2))
            .addProperties("zip", StringSchema()
                    .description("zip code")
                    .pattern("^\\d{5}(?:[-\\s]\\d{4})?$")
                    .minLength(2)
                    .maxLength(2))
            .addProperties("country", StringSchema()
                    ._enum(listOf("US")))
            .description("2-digit country code")
            .minLength(2)
            .maxLength(2)
            .example(Address("test street",
                    "test city",
                    "test state",
                    "333",
                    "US"))

    oai.paths(Paths()
            .addPathItem("/foo", PathItem()
                    .description("the foo path")
                    .get(Operation()
                            .addParametersItem(QueryParameter()
                                    .name("id")
                                    .description("Records to skip")
                                    .required(false)
                                    .schema(IntegerSchema())
                                    .example(1))
                            .responses(ApiResponses()
                                    .addApiResponse("200", ApiResponse()
                                            .description("it worked")
                                            .content(Content()
                                                    .addMediaType("application/json",
                                                            MediaType().schema(Schema<Address>().`$ref`("#/components/schemas/Address"))))
                                            .link("funky", Link().operationId("getFunky")))
                            )
                    )
            )
    )
    return oai
}

@NoArg
data class Address(var street: String?, var city: String?, var state: String?, var zip: String?, var country: String?)

val mapper by lazy {
    val m = ObjectMapper()
    m.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m
}

fun writePrettyJson(value: Any): String {
    return mapper.writer(DefaultPrettyPrinter()).writeValueAsString(value)
}