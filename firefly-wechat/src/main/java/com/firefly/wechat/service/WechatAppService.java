package com.firefly.wechat.service;

import com.firefly.wechat.model.app.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatAppService {

    CompletableFuture<SessionKeyResponse> getSessionKey(SessionKeyRequest request);

    DecryptedUserInfoResponse decryptUserInfo(DecryptedUserInfoRequest request);

    boolean verifySignature(String rawData, String sessionKey, String signature);

    CompletableFuture<List<ByteBuffer>> getCodeUnlimit(CodeUnlimitRequest request, String accessToken);

    CompletableFuture<List<ByteBuffer>> getCode(CodeUnlimitRequest request, String accessToken);

    CompletableFuture<List<ByteBuffer>> createQrcode(QrcodeRequest request, String accessToken);
}
