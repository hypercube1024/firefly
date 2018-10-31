package com.firefly.wechat.service.impl;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.codec.Base64;
import com.firefly.wechat.model.app.*;
import com.firefly.wechat.model.template.SmappTemplateMessageRequest;
import com.firefly.wechat.model.template.TemplateMessageResponse;
import com.firefly.wechat.service.WechatSmallAppService;
import com.firefly.wechat.utils.AesUtils;
import com.firefly.wechat.utils.SHA1;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatSmallAppServiceImpl extends AbstractWechatService implements WechatSmallAppService {

    public WechatSmallAppServiceImpl() {
    }

    public WechatSmallAppServiceImpl(SimpleHTTPClient client) {
        super(client);
    }

    @Override
    public CompletableFuture<SessionKeyResponse> getSessionKey(SessionKeyRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("appid", request.getAppid());
        encoded.put("secret", request.getSecret());
        encoded.put("js_code", request.getJs_code());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/jscode2session", param, SessionKeyResponse.class);
    }

    @Override
    public DecryptedUserInfoResponse decryptUserInfo(DecryptedUserInfoRequest request) {
        byte[] data = AesUtils.decrypt(
                Base64.decodeBase64(request.getEncryptedData()),
                Base64.decodeBase64(request.getSessionKey()),
                Base64.decodeBase64(request.getIv()));
        String userInfo = new String(data, StandardCharsets.UTF_8);

        log.info("decryptUserInfo. user info {}, {}", userInfo, request.getSessionKey());
        return $.json.parse(userInfo, DecryptedUserInfoResponse.class);
    }

    @Override
    public boolean verifySignature(String rawData, String sessionKey, String signature) {
        String sign = SHA1.getSHA1(rawData, sessionKey);
        log.info("decryptUserInfo. sign {}, {}", sign, signature);
        return signature.equals(sign);
    }

    @Override
    public CompletableFuture<List<ByteBuffer>> getCodeUnlimit(CodeUnlimitRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postAndReturnBinaryData("https://api.weixin.qq.com/wxa/getwxacodeunlimit", param, request);
    }

    @Override
    public CompletableFuture<List<ByteBuffer>> getCode(CodeUnlimitRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postAndReturnBinaryData("https://api.weixin.qq.com/wxa/getwxacode", param, request);
    }

    @Override
    public CompletableFuture<List<ByteBuffer>> createQrcode(QrcodeRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postAndReturnBinaryData("https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode", param, request);
    }

    @Override
    public CompletableFuture<CommonMessageResponse> sendCustomerServiceMessage(CommonMessageRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postWechatService("https://api.weixin.qq.com/cgi-bin/message/custom/send", param, request, CommonMessageResponse.class);
    }

    @Override
    public CompletableFuture<TemplateMessageResponse> sendTemplateMessage(SmappTemplateMessageRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postWechatService("https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send", param, request, TemplateMessageResponse.class);
    }
}
