package com.firefly.wechat.service.impl;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.codec.HexUtils;
import com.firefly.wechat.model.ErrorResponse;
import com.firefly.wechat.model.auth.*;
import com.firefly.wechat.service.WechatAuthService;
import com.firefly.wechat.utils.NonceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatAuthServiceImpl extends AbstractWechatService implements WechatAuthService {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public WechatAuthServiceImpl() {

    }

    public WechatAuthServiceImpl(SimpleHTTPClient client) {
        super(client);
    }


    @Override
    public CompletableFuture<AccessTokenResponse> getAccessToken(AccessTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("secret", request.getSecret());
        encoded.put("code", request.getCode());
        encoded.put("grant_type", request.getGrant_type());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/oauth2/access_token", param, AccessTokenResponse.class);
    }

    @Override
    public CompletableFuture<AccessTokenResponse> refreshToken(RefreshTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("refresh_token", request.getRefresh_token());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/oauth2/refresh_token", param, AccessTokenResponse.class);
    }

    @Override
    public CompletableFuture<ErrorResponse> verifyToken(VerifyTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", request.getAccess_token());
        encoded.put("openid", request.getOpenid());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/auth", param, ErrorResponse.class);
    }

    @Override
    public CompletableFuture<WechatUserInfo> getUserInfo(WechatUserInfoRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", request.getAccess_token());
        encoded.put("openid", request.getOpenid());
        encoded.put("lang", request.getLang());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/userinfo", param, WechatUserInfo.class);
    }

    @Override
    public CompletableFuture<ClientAccessTokenResponse> getClientAccessToken(ClientAccessTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("secret", request.getSecret());
        encoded.put("grant_type", request.getGrant_type());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/cgi-bin/token", param, ClientAccessTokenResponse.class);
    }

    @Override
    public CompletableFuture<JsApiTicketResponse> getJsApiTicket(String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        encoded.put("type", "jsapi");
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/cgi-bin/ticket/getticket", param, JsApiTicketResponse.class);
    }

    @Override
    public JsConfigResponse getJsConfig(JsConfigRequest request) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = NonceUtils.generateNonce();
        StringBuilder signStr = new StringBuilder();
        signStr.append("jsapi_ticket=").append(request.getTicket()).append("&")
               .append("noncestr=").append(nonce).append("&")
               .append("timestamp=").append(timestamp).append("&")
               .append("url=").append(request.getUrl());
        String s = signStr.toString();
        log.info("getJsConfig. origin sign -> {}", s);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(s.getBytes());
            byte[] digest = md.digest();

            JsConfigResponse response = new JsConfigResponse();
            response.setAppId(request.getAppId());
            response.setNonceStr(nonce);
            response.setTimestamp(timestamp);
            response.setSignature(HexUtils.bytesToHex(digest));
            return response;
        } catch (Exception e) {
            log.error("getJsConfig sha1 error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ApiAccessTokenResponse> getApiAccessToken(ApiAccessTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("appid", request.getAppid());
        encoded.put("secret", request.getSecret());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/cgi-bin/token", param, ApiAccessTokenResponse.class);
    }

    @Override
    public String buildAuthorizedUrl(AuthorizedUrlRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("redirect_uri", request.getRedirectUri());
        encoded.put("response_type", "code");
        encoded.put("scope", request.getScope());
        encoded.put("state", request.getState());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?" + param + "#wechat_redirect";
    }
}
