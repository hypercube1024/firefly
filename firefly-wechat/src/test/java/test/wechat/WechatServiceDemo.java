package test.wechat;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.wechat.model.EchoRequest;
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
        wechatService = service;
    }

    public static void main(String[] args) {
        $.httpServer().router().methods(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}).path("/")
         .handler(ctx -> {
             EchoRequest request = new EchoRequest();
             request.setEchostr(ctx.getParameter("echostr"));
             request.setNonce(ctx.getParameter("nonce"));
             request.setSignature(ctx.getParameter("signature"));
             request.setTimestamp(ctx.getParamOpt("timestamp").map(Long::parseLong).orElse(0L));

             try {
                 if (wechatService.verifyEchoString(request)) {
                     ctx.end(request.getEchostr());
                 } else {
                     ctx.end("success");
                 }
             } catch (IllegalArgumentException e) {
                 ctx.end("success");
             }
         })
         .listen("localhost", 8080);
    }
}
