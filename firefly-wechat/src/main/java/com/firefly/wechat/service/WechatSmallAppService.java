package com.firefly.wechat.service;

import com.firefly.wechat.model.app.*;
import com.firefly.wechat.model.template.SmappTemplateMessageRequest;
import com.firefly.wechat.model.template.TemplateMessageResponse;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The wechat small app service.
 *
 * @author Pengtao Qiu
 */
public interface WechatSmallAppService {

    CompletableFuture<SessionKeyResponse> getSessionKey(SessionKeyRequest request);

    DecryptedUserInfoResponse decryptUserInfo(DecryptedUserInfoRequest request);

    boolean verifySignature(String rawData, String sessionKey, String signature);

    CompletableFuture<List<ByteBuffer>> getCodeUnlimit(CodeUnlimitRequest request, String accessToken);

    CompletableFuture<List<ByteBuffer>> getCode(CodeUnlimitRequest request, String accessToken);

    CompletableFuture<List<ByteBuffer>> createQrcode(QrcodeRequest request, String accessToken);

    CompletableFuture<CommonMessageResponse> sendCustomerServiceMessage(CommonMessageRequest request, String accessToken);

    CompletableFuture<TemplateMessageResponse> sendTemplateMessage(SmappTemplateMessageRequest request, String accessToken);
}
