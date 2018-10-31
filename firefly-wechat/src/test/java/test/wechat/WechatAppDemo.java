package test.wechat;

import com.firefly.$;
import com.firefly.wechat.model.app.DecryptedUserInfoRequest;
import com.firefly.wechat.model.app.DecryptedUserInfoResponse;
import com.firefly.wechat.model.app.SessionKeyRequest;
import com.firefly.wechat.model.app.SessionKeyResponse;
import com.firefly.wechat.service.WechatSmallAppService;
import com.firefly.wechat.service.impl.WechatSmallAppServiceImpl;

/**
 * @author Pengtao Qiu
 */
public class WechatAppDemo {
    public static void main(String[] args) throws Exception {
        WechatSmallAppService wechatAppService = new WechatSmallAppServiceImpl($.httpsClient());

        SessionKeyRequest request = new SessionKeyRequest();
        request.setAppid("");
        request.setSecret("");
        request.setJs_code("001fdPt52O2SiK0GrAu52Ipst52fdPtB");
        SessionKeyResponse response = wechatAppService.getSessionKey(request).get();
        System.out.println(response);

        DecryptedUserInfoRequest userInfoRequest = new DecryptedUserInfoRequest();
        userInfoRequest.setEncryptedData("cfM6EjiTUXkkISFCJXEmazGpW/Trtuuxfhe65WBuzBnb54FZAr8JshJhmfIms+33gx3zmNydl09l6tavvsgFP3UDuPiHCPTY/LyfWN1GXXEzkrpK2Ony1PGh9G3A1YuwionYM21AwNnOr/R2qBH40Pz9cS1bGJadr5NZkuS4v5hGYKIE7DJChlK+pPGgIFDVQ3DMfaDoYm27rmTmEMK/EFAPqTe598IRekYPzdqTjazvHEIjFz0CtzUga1Mab10UgKtP0kCxe30ImwQo3DYXnajCYXeLvz63OaLLN17gzTS8WYhpDOyDYnuB7Vwwx06niXZAsuBduDbSyMKQ5clU2N+hT35q6OnA0xPG5l4sdA5EW9KvZGAFyhvH7VpNLPyKEToZVxTohNxRcUQimqihbGDtQPd+ap7okH5qhJUk6AaMukdi/4NxC8hhdw+wwDwjbEPDou8V7mt42RkWg7V60w==");
        userInfoRequest.setIv("/+0Hr0iPbZUSwNhUNPfL4Q==");
        userInfoRequest.setSessionKey(response.getSession_key());
        DecryptedUserInfoResponse userInfoResponse = wechatAppService.decryptUserInfo(userInfoRequest);
        System.out.println(userInfoResponse);

        boolean success = wechatAppService.verifySignature("{\"nickName\":\"Alvin\",\"gender\":1,\"language\":\"zh_CN\",\"city\":\"Wuhan\",\"province\":\"Hubei\",\"country\":\"China\",\"avatarUrl\":\"https://wx.qlogo.cn/mmopen/vi_32/DYAIOgq83ersyMXIpN4H9wj2Fps4ZEd30yZIMSc3icw1aPsfEegv2IkBoWNBCrZmEYjuYksbgz3lciaq23bYKesw/0\"}",
                response.getSession_key(), "a5f1662e83c3a8b9e47850a0d392e7c3a4885aa2");
        System.out.println(success);
    }
}
