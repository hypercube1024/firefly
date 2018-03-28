package test.wechat;

import com.firefly.$;
import com.firefly.wechat.model.auth.*;
import com.firefly.wechat.model.user.UserListRequest;
import com.firefly.wechat.model.user.UserListResponse;
import com.firefly.wechat.service.WechatAuthService;
import com.firefly.wechat.service.WechatUserService;
import com.firefly.wechat.service.impl.WechatAuthServiceImpl;
import com.firefly.wechat.service.impl.WechatUserServiceImpl;

/**
 * @author Pengtao Qiu
 */
public class WechatAuthDemo {
    public static void main(String[] args) throws Exception {
        WechatAuthService authService = new WechatAuthServiceImpl($.httpsClient());
        WechatUserService userService = new WechatUserServiceImpl($.httpsClient());
        ApiAccessTokenRequest tokenRequest = new ApiAccessTokenRequest();
        tokenRequest.setAppid("wxef8f7b5d26905586");
        tokenRequest.setSecret("b6748ee54eae916861c08983ba2b5f2d");
        ApiAccessTokenResponse tokenResponse = authService.getApiAccessToken(tokenRequest).get();
        System.out.println(tokenResponse);

        UserListRequest request = new UserListRequest();
        request.setAccess_token(tokenResponse.getAccess_token());
        UserListResponse response = userService.getUsers(request).get();
        System.out.println(response);
    }
}
