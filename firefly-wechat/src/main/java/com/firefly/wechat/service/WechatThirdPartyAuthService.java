package com.firefly.wechat.service;

import com.firefly.wechat.model.auth.AccessTokenResponse;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyAccessTokenRequest;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyAuthorizedUrlRequest;
import com.firefly.wechat.model.thirdparty.auth.ThirdPartyRefreshTokenRequest;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatThirdPartyAuthService {

    String buildAuthorizedUrl(ThirdPartyAuthorizedUrlRequest request);

    CompletableFuture<AccessTokenResponse> getAccessToken(ThirdPartyAccessTokenRequest request);

    CompletableFuture<AccessTokenResponse> refreshToken(ThirdPartyRefreshTokenRequest request);
}
