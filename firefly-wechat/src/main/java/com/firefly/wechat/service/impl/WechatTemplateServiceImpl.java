package com.firefly.wechat.service.impl;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.wechat.model.template.TemplateListResponse;
import com.firefly.wechat.model.template.TemplateMessageRequest;
import com.firefly.wechat.model.template.TemplateMessageResponse;
import com.firefly.wechat.service.WechatTemplateService;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatTemplateServiceImpl extends AbstractWechatService implements WechatTemplateService {

    public WechatTemplateServiceImpl() {

    }

    public WechatTemplateServiceImpl(SimpleHTTPClient client) {
        super(client);
    }

    @Override
    public CompletableFuture<TemplateListResponse> listTemplates(String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/cgi-bin/template/get_all_private_template",
                param, TemplateListResponse.class);
    }

    @Override
    public CompletableFuture<TemplateMessageResponse> sendMessage(TemplateMessageRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postWechatService("https://api.weixin.qq.com/cgi-bin/message/template/send",
                param, request, TemplateMessageResponse.class);
    }

}
