package com.firefly.wechat.service;

import com.firefly.wechat.model.ErrorResponse;
import com.firefly.wechat.model.auth.*;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatAuthService {

    CompletableFuture<AccessTokenResponse> getAccessToken(AccessTokenRequest request);

    CompletableFuture<AccessTokenResponse> refreshToken(RefreshTokenRequest request);

    CompletableFuture<ErrorResponse> verifyToken(VerifyTokenRequest request);

    CompletableFuture<WechatUserInfo> getUserInfo(WechatUserInfoRequest request);

    CompletableFuture<ClientAccessTokenResponse> getClientAccessToken(ClientAccessTokenRequest request);

    CompletableFuture<JsApiTicketResponse> getJsApiTicket(String accessToken);

    JsConfigResponse getJsConfig(JsConfigRequest request);

    CompletableFuture<ApiAccessTokenResponse> getApiAccessToken(ApiAccessTokenRequest request);
}
