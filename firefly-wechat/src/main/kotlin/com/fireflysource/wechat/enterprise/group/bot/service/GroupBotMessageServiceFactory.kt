package com.fireflysource.wechat.enterprise.group.bot.service

import com.fireflysource.f
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.wechat.enterprise.group.bot.service.impl.AsyncGroupBotMessageService

/**
 * @author Pengtao Qiu
 */
object GroupBotMessageServiceFactory {

    @JvmOverloads
    fun create(webHookUrl: String, httpClient: HttpClient = f.httpClient()): GroupBotMessageService {
        return AsyncGroupBotMessageService(webHookUrl, httpClient)
    }
}