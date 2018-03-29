package com.firefly.wechat.service;

import com.firefly.wechat.model.template.TemplateListResponse;
import com.firefly.wechat.model.template.TemplateMessageRequest;
import com.firefly.wechat.model.template.TemplateMessageResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatTemplateService {

    CompletableFuture<TemplateListResponse> listTemplates(String accessToken);

    CompletableFuture<TemplateMessageResponse> sendMessage(TemplateMessageRequest request, String accessToken);
}
