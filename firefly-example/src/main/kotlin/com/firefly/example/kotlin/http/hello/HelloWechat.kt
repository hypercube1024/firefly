package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.utils.codec.HexUtils
import java.security.MessageDigest

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val wechatToken = "xxxxddd"
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler {
                val echoStr = getParamOpt("echostr").orElse("")
                val signature = getParamOpt("signature").orElse("")
                val nonce = getParamOpt("nonce").orElse("")
                val timestamp = getParamOpt("timestamp").orElse("")

                if (echoStr != "" && signature != "" && nonce != "" && timestamp != "") {
                    val paramArray = arrayOf(nonce, timestamp, wechatToken).sortedArray()
                    val sign = StringBuilder()
                    paramArray.forEach { sign.append(it) }

                    val hexSign =
                        HexUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(sign.toString().toByteArray()))
                    println("verify wechat token $sign | $hexSign | $signature")
                    if (hexSign == signature) {
                        end(echoStr)
                    } else {
                        end("success")
                    }
                } else {
                    end("success")
                }
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