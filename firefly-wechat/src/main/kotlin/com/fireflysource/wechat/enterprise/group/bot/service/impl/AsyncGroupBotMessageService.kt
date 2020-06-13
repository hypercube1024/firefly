package com.fireflysource.wechat.enterprise.group.bot.service.impl

import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.wechat.enterprise.group.bot.model.GroupBotMessageResult
import com.fireflysource.wechat.enterprise.group.bot.model.Message
import com.fireflysource.wechat.enterprise.group.bot.service.GroupBotMessageService
import com.fireflysource.wechat.utils.JsonUtils
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class AsyncGroupBotMessageService(
    private val webHookUrl: String,
    private val httpClient: HttpClient
) : GroupBotMessageService {

    override fun sendMessage(message: Message): CompletableFuture<GroupBotMessageResult> {
        val json = JsonUtils.write(message)
        return httpClient.post(webHookUrl)
            .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
            .body(json)
            .submit()
            .thenApply { JsonUtils.read<GroupBotMessageResult>(it.stringBody) }
    }
}