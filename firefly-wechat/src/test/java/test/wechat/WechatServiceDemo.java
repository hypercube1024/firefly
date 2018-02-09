package test.wechat;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.wechat.service.WechatMessageService;
import com.firefly.wechat.service.impl.WechatMessageServiceImpl;

/**
 * @author Pengtao Qiu
 */
public class WechatServiceDemo {

    private static WechatMessageService wechatMessageService;

    static {
        WechatMessageServiceImpl service = new WechatMessageServiceImpl();
        service.setWechatToken("myTest123456");
        service.setAppId("12345");
        service.setAesKey("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG");
        wechatMessageService = service;
    }

    public static void main(String[] args) {
        $.httpServer().router().methods(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}).path("/")
         .handler(wechatMessageService::onRequest)
         .listen("localhost", 8080);
    }
}
