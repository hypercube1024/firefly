package test.wechat;

import com.firefly.$;
import com.firefly.wechat.model.auth.ApiAccessTokenRequest;
import com.firefly.wechat.model.auth.ApiAccessTokenResponse;
import com.firefly.wechat.model.user.*;
import com.firefly.wechat.service.WechatAuthService;
import com.firefly.wechat.service.WechatUserService;
import com.firefly.wechat.service.impl.WechatAuthServiceImpl;
import com.firefly.wechat.service.impl.WechatUserServiceImpl;

import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class WechatUserServiceDemo {
    public static void main(String[] args) throws Exception {
        WechatAuthService authService = new WechatAuthServiceImpl($.httpsClient());
        WechatUserService userService = new WechatUserServiceImpl($.httpsClient());

        ApiAccessTokenRequest tokenRequest = new ApiAccessTokenRequest();
        tokenRequest.setAppid("");
        tokenRequest.setSecret("");
        ApiAccessTokenResponse tokenResponse = authService.getApiAccessToken(tokenRequest).get();
        System.out.println(tokenResponse);

        UserListRequest userListRequest = new UserListRequest();
        userListRequest.setAccess_token(tokenResponse.getAccess_token());
        UserListResponse userListResponse = userService.getUsers(userListRequest).get();
        System.out.println(userListResponse);

        BatchUserInfoRequest batchUserInfoRequest = new BatchUserInfoRequest();
        batchUserInfoRequest.setUser_list(userListResponse.getData().getOpenid().stream().map(openId -> {
            UserId userId = new UserId();
            userId.setLang("zh_CN");
            userId.setOpenid(openId);
            return userId;
        }).collect(Collectors.toList()));
        BatchUserInfoResponse batchUserInfoResponse = userService.getUserInfoBatch(batchUserInfoRequest, tokenResponse.getAccess_token()).get();
        System.out.println(batchUserInfoResponse);
    }
}
