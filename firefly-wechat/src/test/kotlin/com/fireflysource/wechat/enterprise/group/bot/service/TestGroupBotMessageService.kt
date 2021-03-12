package com.fireflysource.wechat.enterprise.group.bot.service

import com.fireflysource.fx
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.serialization.SerializationServiceFactory.json
import com.fireflysource.wechat.enterprise.group.bot.model.GroupBotMessageResult
import com.fireflysource.wechat.enterprise.group.bot.model.MessageBuilder
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.random.Random

/**
 * @author Pengtao Qiu
 */
class TestGroupBotMessageService {

    companion object {

        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments(
                    "/cgi-bin/webhook/send",
                    GroupBotMessageResult(
                        93000,
                        "invalid webhook url, hint: [1592021214_50_551fc8807c11f27ed0e6714d0c267ba4], from ip: 171.43.164.231, more info at https://open.work.weixin.qq.com/devtool/query?e=93000"
                    )
                ),
                arguments("/cgi-bin/webhook/send?key=testKey", GroupBotMessageResult(0, "ok")),
                arguments(
                    "/cgi-bin/webhook/send?key=xxx", GroupBotMessageResult(
                        93000,
                        "invalid webhook url, hint: [1592021214_50_551fc8807c11f27ed0e6714d0c267ba4], from ip: 171.43.164.231, more info at https://open.work.weixin.qq.com/devtool/query?e=93000"
                    )
                ),
                arguments(
                    "/cgi-bin/webhook/sendFFFFF", GroupBotMessageResult(
                        -99999,
                        "The http request failure."
                    )
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should send wechat enterprise group bot message successfully.")
    fun test(path: String, expectedResult: GroupBotMessageResult) = runBlocking {
        val host = "localhost"
        val port = Random.nextInt(10000, 20000)
        val webHookUrl = "https://$host:$port$path"

        val server = createMockServer()
        val client = fx.createHttpClient()
        server.listen(host, port)

        val service = GroupBotMessageServiceFactory.create(webHookUrl, client)
        val message = MessageBuilder.text().content("hello world").end()
        val result = service.sendMessage(message).await()
        assertEquals(expectedResult, result)

        client.stop()
        server.stop()
    }

    private fun createMockServer(): HttpServer {
        return fx.createHttpServer()
            .router()
            .post("/cgi-bin/webhook/send")
            .handler { ctx ->
                val key = ctx.getQueryString("key")
                val result = when {
                    key.isNullOrBlank() || key != "testKey" -> GroupBotMessageResult(
                        93000,
                        "invalid webhook url, hint: [1592021214_50_551fc8807c11f27ed0e6714d0c267ba4], from ip: 171.43.164.231, more info at https://open.work.weixin.qq.com/devtool/query?e=93000"
                    )
                    else -> GroupBotMessageResult(0, "ok")
                }

                ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value).end(json.write(result))
            }
            .enableSecureConnection()
    }
}