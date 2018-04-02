package com.firefly.wechat.service;

import com.firefly.wechat.model.app.DecryptedUserInfoRequest;
import com.firefly.wechat.model.app.DecryptedUserInfoResponse;
import com.firefly.wechat.model.app.SessionKeyRequest;
import com.firefly.wechat.model.app.SessionKeyResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatAppService {

    CompletableFuture<SessionKeyResponse> getSessionKey(SessionKeyRequest request);

    DecryptedUserInfoResponse decryptUserInfo(DecryptedUserInfoRequest request);

    boolean verifySignature(String rawData, String sessionKey, String signature);
}
