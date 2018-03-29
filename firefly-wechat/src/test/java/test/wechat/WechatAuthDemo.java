package test.wechat;

import com.firefly.$;
import com.firefly.wechat.model.auth.ApiAccessTokenRequest;
import com.firefly.wechat.model.auth.ApiAccessTokenResponse;
import com.firefly.wechat.model.template.TemplateData;
import com.firefly.wechat.model.template.TemplateListResponse;
import com.firefly.wechat.model.template.TemplateMessageRequest;
import com.firefly.wechat.model.user.UserListRequest;
import com.firefly.wechat.model.user.UserListResponse;
import com.firefly.wechat.service.WechatAuthService;
import com.firefly.wechat.service.WechatTemplateService;
import com.firefly.wechat.service.WechatUserService;
import com.firefly.wechat.service.impl.WechatAuthServiceImpl;
import com.firefly.wechat.service.impl.WechatTemplateServiceImpl;
import com.firefly.wechat.service.impl.WechatUserServiceImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class WechatAuthDemo {
    public static void main(String[] args) throws Exception {
        WechatAuthService authService = new WechatAuthServiceImpl($.httpsClient());
        WechatUserService userService = new WechatUserServiceImpl($.httpsClient());
        WechatTemplateService templateService = new WechatTemplateServiceImpl($.httpsClient());

        ApiAccessTokenRequest tokenRequest = new ApiAccessTokenRequest();
        tokenRequest.setAppid("");
        tokenRequest.setSecret("");
        ApiAccessTokenResponse tokenResponse = authService.getApiAccessToken(tokenRequest).get();
        System.out.println(tokenResponse);

        UserListRequest userListRequest = new UserListRequest();
        userListRequest.setAccess_token(tokenResponse.getAccess_token());
        UserListResponse userListResponse = userService.getUsers(userListRequest).get();
        System.out.println(userListResponse);

        TemplateListResponse templateListResponse = templateService.listTemplates(tokenResponse.getAccess_token()).get();
        System.out.println(templateListResponse);

        userListResponse.getData().getOpenid().forEach(openId -> {
            TemplateMessageRequest templateMessageRequest = new TemplateMessageRequest();
            templateMessageRequest.setTouser(openId);
            templateMessageRequest.setTemplate_id(templateListResponse.getTemplate_list().get(0).getTemplate_id());
            templateMessageRequest.setUrl("http://www.fireflysource.com");

            Map<String, TemplateData> data = new HashMap<>();
            TemplateData v1 = new TemplateData();
            v1.setValue("mmp🐶🤡🙄🐣");
            data.put("key1", v1);

            TemplateData v2 = new TemplateData();
            v2.setValue("哦咧咧😯");
            data.put("key2", v2);
            templateMessageRequest.setData(data);
            templateService.sendMessage(templateMessageRequest, tokenResponse.getAccess_token())
                           .thenAccept(System.out::println);
        });

    }
}
