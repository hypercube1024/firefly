package com.firefly.wechat.service.impl;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.wechat.model.auth.AccessTokenResponse;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyAccessTokenRequest;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyAuthorizedUrlRequest;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyRefreshTokenRequest;
import com.firefly.wechat.service.WechatThirdPartyAuthService;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatThirdPartyAuthServiceImpl extends AbstractWechatService implements WechatThirdPartyAuthService {

    public WechatThirdPartyAuthServiceImpl() {
    }

    public WechatThirdPartyAuthServiceImpl(SimpleHTTPClient client) {
        super(client);
    }

    @Override
    public String buildAuthorizedUrl(ThirdPartyAuthorizedUrlRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("redirect_uri", request.getRedirectUri());
        encoded.put("response_type", "code");
        encoded.put("scope", request.getScope());
        encoded.put("state", request.getState());
        encoded.put("component_appid", request.getComponentAppid());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?" + param + "#wechat_redirect";
    }

    @Override
    public CompletableFuture<AccessTokenResponse> getAccessToken(ThirdPartyAccessTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("code", request.getCode());
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("component_appid", request.getComponent_appid());
        encoded.put("component_access_token", request.getComponent_access_token());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/oauth2/component/access_token", param, AccessTokenResponse.class);
    }

    @Override
    public CompletableFuture<AccessTokenResponse> refreshToken(ThirdPartyRefreshTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("refresh_token", request.getRefresh_token());
        encoded.put("component_appid", request.getComponent_appid());
        encoded.put("component_access_token", request.getComponent_access_token());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/sns/oauth2/component/refresh_token", param, AccessTokenResponse.class);
    }
}
