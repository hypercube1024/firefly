package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.utils.codec.HexUtils
import java.security.MessageDigest

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    println(sortedSetOf("timestamp", "nonce", "token"))
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler {
                val echoStr = getParameter("echostr")
                val token = "myTest123456"
                val signature = getParameter("signature")


                val sign = "${getParameter("nonce")}${getParameter("timestamp")}$token"

                val hexSign = HexUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(sign.toByteArray()))
                println("$hexSign, $signature, $echoStr")
                end(echoStr)
            }
        }

        router {
            httpMethod = HttpMethod.POST
            path = "/"

            asyncHandler {
                println(stringBody)
                end("success")
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/helloWechat"

            asyncHandler {
                end("hello")
            }
        }
    }.listen("localhost", 8080)
}