package com.firefly.wechat.service.impl;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.Base64;
import com.firefly.wechat.model.app.DecryptedUserInfoRequest;
import com.firefly.wechat.model.app.DecryptedUserInfoResponse;
import com.firefly.wechat.model.app.SessionKeyRequest;
import com.firefly.wechat.model.app.SessionKeyResponse;
import com.firefly.wechat.service.WechatAppService;
import com.firefly.wechat.utils.AesUtils;
import com.firefly.wechat.utils.SHA1;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatAppServiceImpl extends AbstractWechatService implements WechatAppService {

    public WechatAppServiceImpl() {
    }

    public WechatAppServiceImpl(SimpleHTTPClient client) {
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
}
