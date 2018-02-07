package test.wechat;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.wechat.service.WechatService;
import com.firefly.wechat.service.impl.WechatServiceImpl;

/**
 * @author Pengtao Qiu
 */
public class WechatServiceDemo {

    private static WechatService wechatService;

    static {
        WechatServiceImpl service = new WechatServiceImpl();
        service.setWechatToken("myTest123456");
        service.setAppId("wxef8f7b5d26905586");
        service.setAesKey("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG");
        service.addEchoStringListener((req, ctx) -> ctx.end(req.getEchostr()));
        service.addTextMessageListener(((msgReq, text, ctx) -> ctx.end(wechatService.encryptMessage("success", msgReq.getTimestamp(), msgReq.getNonce()))));
        wechatService = service;
    }

    public static void main(String[] args) {
        $.httpServer().router().methods(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}).path("/")
         .handler(wechatService::onRequest)
         .listen("localhost", 8080);
    }
}
