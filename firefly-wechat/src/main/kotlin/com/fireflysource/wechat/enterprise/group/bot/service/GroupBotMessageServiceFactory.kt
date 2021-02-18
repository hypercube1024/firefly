package com.fireflysource.wechat.enterprise.group.bot.service

import com.fireflysource.fx
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.wechat.enterprise.group.bot.service.impl.AsyncGroupBotMessageService

/**
 * @author Pengtao Qiu
 */
object GroupBotMessageServiceFactory {

    @JvmOverloads
    fun create(webHookUrl: String, httpClient: HttpClient = fx.httpClient()): GroupBotMessageService {
        return AsyncGroupBotMessageService(webHookUrl, httpClient)
    }
}