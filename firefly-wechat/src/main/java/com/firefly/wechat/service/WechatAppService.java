package com.firefly.wechat.service;

import com.firefly.wechat.model.app.SessionKeyRequest;
import com.firefly.wechat.model.app.SessionKeyResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatAppService {

    CompletableFuture<SessionKeyResponse> getSessionKey(SessionKeyRequest request);

}
