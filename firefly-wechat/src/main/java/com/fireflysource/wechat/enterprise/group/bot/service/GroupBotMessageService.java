package com.fireflysource.wechat.enterprise.group.bot.service;

import com.fireflysource.wechat.enterprise.group.bot.model.GroupBotMessageResult;
import com.fireflysource.wechat.enterprise.group.bot.model.Message;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface GroupBotMessageService {

    CompletableFuture<GroupBotMessageResult> sendMessage(Message message);

}
