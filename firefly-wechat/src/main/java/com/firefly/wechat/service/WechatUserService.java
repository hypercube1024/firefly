package com.firefly.wechat.service;

import com.firefly.wechat.model.user.BatchUserInfoRequest;
import com.firefly.wechat.model.user.BatchUserInfoResponse;
import com.firefly.wechat.model.user.UserListRequest;
import com.firefly.wechat.model.user.UserListResponse;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface WechatUserService {

    CompletableFuture<UserListResponse> getUsers(UserListRequest request);

    CompletableFuture<BatchUserInfoResponse> getUserInfoBatch(BatchUserInfoRequest request, String accessToken);
}
