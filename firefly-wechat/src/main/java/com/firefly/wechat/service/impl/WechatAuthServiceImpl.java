package com.firefly.wechat.service.impl;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.wechat.model.ErrorResponse;
import com.firefly.wechat.model.auth.*;
import com.firefly.wechat.service.WechatAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatAuthServiceImpl implements WechatAuthService {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private SimpleHTTPClient client;

    public WechatAuthServiceImpl() {

    }

    public WechatAuthServiceImpl(SimpleHTTPClient client) {
        this.client = client;
    }

    public SimpleHTTPClient getClient() {
        return client;
    }

    public void setClient(SimpleHTTPClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<AccessTokenResponse> getAccessToken(AccessTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("secret", request.getSecret());
        encoded.put("code", request.getCode());
        encoded.put("grant_type", request.getGrant_type());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        CompletableFuture<AccessTokenResponse> ret = new CompletableFuture<>();
        client.get("https://api.weixin.qq.com/sns/oauth2/access_token?" + param).submit()
              .thenAccept(res -> {
                  log.info("call getAccessToken -> {}, {}, {}", param, res.getStatus(), res.getStringBody());
                  completeAccessToken(ret, res);
              });
        return ret;
    }

    @Override
    public CompletableFuture<AccessTokenResponse> refreshToken(RefreshTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("appid", request.getAppid());
        encoded.put("grant_type", request.getGrant_type());
        encoded.put("refresh_token", request.getRefresh_token());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        CompletableFuture<AccessTokenResponse> ret = new CompletableFuture<>();
        client.get("https://api.weixin.qq.com/sns/oauth2/refresh_token?" + param).submit()
              .thenAccept(res -> {
                  log.info("call refreshToken -> {}, {}, {}", param, res.getStatus(), res.getStringBody());
                  completeAccessToken(ret, res);
              });
        return ret;
    }

    protected void completeAccessToken(CompletableFuture<AccessTokenResponse> ret, SimpleResponse res) {
        if (res.getStatus() == HttpStatus.OK_200) {
            if (res.getJsonObjectBody().getInteger("errcode") != 0) {
                ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
                ret.completeExceptionally(errorResponse);
            } else {
                AccessTokenResponse response = res.getJsonBody(AccessTokenResponse.class);
                ret.complete(response);
            }
        } else {
            ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
            ret.completeExceptionally(errorResponse);
        }
    }

    @Override
    public CompletableFuture<ErrorResponse> verifyToken(VerifyTokenRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", request.getAccess_token());
        encoded.put("openid", request.getOpenid());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        CompletableFuture<ErrorResponse> ret = new CompletableFuture<>();
        client.get("https://api.weixin.qq.com/sns/auth?" + param).submit()
              .thenAccept(res -> {
                  log.info("call verifyToken -> {}, {}, {}", param, res.getStatus(), res.getStringBody());
                  ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
                  ret.complete(errorResponse);
              });
        return ret;
    }

    @Override
    public CompletableFuture<WechatUserInfo> getUserInfo(WechatUserInfoRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", request.getAccess_token());
        encoded.put("openid", request.getOpenid());
        encoded.put("lang", request.getLang());
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        CompletableFuture<WechatUserInfo> ret = new CompletableFuture<>();
        client.get("https://api.weixin.qq.com/sns/userinfo?" + param).submit()
              .thenAccept(res -> {
                  log.info("call getUserInfo -> {}, {}, {}", param, res.getStatus(), res.getStringBody());
                  if (res.getStatus() == HttpStatus.OK_200) {
                      if (res.getJsonObjectBody().getInteger("errcode") != 0) {
                          ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
                          ret.completeExceptionally(errorResponse);
                      } else {
                          ret.complete(res.getJsonBody(WechatUserInfo.class));
                      }
                  } else {
                      ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
                      ret.completeExceptionally(errorResponse);
                  }
              });
        return ret;
    }
}
