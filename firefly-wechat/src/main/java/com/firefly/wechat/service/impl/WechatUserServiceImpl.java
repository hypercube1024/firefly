package com.firefly.wechat.service.impl;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.wechat.model.user.BatchUserInfoRequest;
import com.firefly.wechat.model.user.BatchUserInfoResponse;
import com.firefly.wechat.model.user.UserListRequest;
import com.firefly.wechat.model.user.UserListResponse;
import com.firefly.wechat.service.WechatUserService;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class WechatUserServiceImpl extends AbstractWechatService implements WechatUserService {


    public WechatUserServiceImpl() {
    }

    public WechatUserServiceImpl(SimpleHTTPClient client) {
        super(client);
    }

    @Override
    public CompletableFuture<UserListResponse> getUsers(UserListRequest request) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", request.getAccess_token());
        if ($.string.hasText(request.getNext_openid())) {
            encoded.put("next_openid", request.getNext_openid());
        }
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return callWechatService("https://api.weixin.qq.com/cgi-bin/user/get", param, UserListResponse.class);
    }

    @Override
    public CompletableFuture<BatchUserInfoResponse> getUserInfoBatch(BatchUserInfoRequest request, String accessToken) {
        UrlEncoded encoded = new UrlEncoded();
        encoded.put("access_token", accessToken);
        String param = encoded.encode(StandardCharsets.UTF_8, true);
        return postWechatService("https://api.weixin.qq.com/cgi-bin/user/info/batchget", param, request, BatchUserInfoResponse.class);
    }

}
