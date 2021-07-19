package com.fireflysource.wechat.enterprise.group.bot.model

import com.fireflysource.serialization.SerializationServiceFactory.json
import com.fireflysource.serialization.impl.json.read
import com.fireflysource.wechat.enterprise.group.bot.model.MessageBuilder.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * @author Pengtao Qiu
 */
class TestMessageModel {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments(
                    """
                    {
                        "msgtype": "text",
                        "text": {
                            "content": "hello world"
                        }
                    }
                """.trimIndent(),
                    text().content("hello world").end()
                ),
                arguments(
                    """
                    {
                        "msgtype": "text",
                        "text": {
                            "content": "广州今日天气：29度，大部分多云，降雨概率：60%",
                            "mentioned_list":["wangqing","@all"],
                            "mentioned_mobile_list":["13800001111","@all"]
                        }
                    }
                """.trimIndent(),
                    text().content("广州今日天气：29度，大部分多云，降雨概率：60%")
                        .mentionedList(listOf("wangqing", "@all"))
                        .mentionedMobileList(listOf("13800001111", "@all"))
                        .end()
                ),
                arguments(
                    "{\"markdown\":{\"content\":\"实时新增用户反馈<font color=\\\"warning\\\">132例</font>，请相关同事注意。\\n>类型:<font color=\\\"comment\\\">用户反馈</font> \\n>普通用户反馈:<font color=\\\"comment\\\">117例</font> \\n>VIP用户反馈:<font color=\\\"comment\\\">15例</font>\"},\"msgtype\":\"markdown\"}",
                    markdown().content(
                        """
                |实时新增用户反馈<font color="warning">132例</font>，请相关同事注意。
                |>类型:<font color="comment">用户反馈</font> 
                |>普通用户反馈:<font color="comment">117例</font> 
                |>VIP用户反馈:<font color="comment">15例</font>
            """.trimMargin()
                    )
                        .end()
                ),
                arguments(
                    """
                    {
                        "msgtype": "image",
                        "image": {
                            "base64": "DATA",
                            "md5": "MD5"
                        }
                    }
                """.trimIndent(),
                    image().base64("DATA").md5("MD5").end()
                ),
                arguments(
                    """
                    {
                        "msgtype": "news",
                        "news": {
                           "articles" : [
                               {
                                   "title" : "中秋节礼品领取",
                                   "description" : "今年中秋节公司有豪礼相送",
                                   "url" : "URL",
                                   "picurl" : "http://res.mail.qq.com/node/ww/wwopenmng/images/independent/doc/test_pic_msg1.png"
                               }
                            ]
                        }
                    }
                """.trimIndent(),
                    news().addArticle(
                        Article(
                            "中秋节礼品领取",
                            "今年中秋节公司有豪礼相送",
                            "URL",
                            "http://res.mail.qq.com/node/ww/wwopenmng/images/independent/doc/test_pic_msg1.png"
                        )
                    )
                        .end()
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should read wechat enterprise group bot message successfully.")
    fun test(string: String, message: Message) {
        val m = when (message.messageType) {
            MessageType.TEXT -> json.read<TextMessage>(string)
            MessageType.MARKDOWN -> json.read<MarkdownMessage>(string)
            MessageType.IMAGE -> json.read<ImageMessage>(string)
            MessageType.NEWS -> json.read<NewsMessage>(string)
            else -> null
        }
        assertEquals(message, m)
    }

}