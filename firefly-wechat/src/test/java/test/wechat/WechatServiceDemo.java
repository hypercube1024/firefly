package test.wechat;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.wechat.model.CommonRequest;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.MessageRequest;
import com.firefly.wechat.model.message.TextMessage;
import com.firefly.wechat.service.WechatService;
import com.firefly.wechat.service.impl.WechatServiceImpl;
import com.firefly.wechat.utils.CtxUtils;
import com.firefly.wechat.utils.MessageUtils;
import com.firefly.wechat.utils.WXBizMsgCrypt;

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
        wechatService = service;
    }

    public static void main(String[] args) {
        $.httpServer().router().methods(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}).path("/")
         .handler(ctx -> {
             CommonRequest commonRequest = CtxUtils.toRequest(ctx);

             if (commonRequest instanceof EchoRequest) {
                 EchoRequest echoRequest = (EchoRequest)commonRequest;
                 if (wechatService.verifyEchoString(echoRequest)) {
                     ctx.end(echoRequest.getEchostr());
                 } else {
                     ctx.end("success");
                 }
             } else if (commonRequest instanceof MessageRequest) {
                 MessageRequest messageRequest = (MessageRequest) commonRequest;
                 System.out.println(ctx.getMethod());
                 System.out.println(ctx.getURI());

                 String fromXML = ctx.getStringBody();
                 System.out.println("encrypted: " + fromXML);

                 try {
                     WXBizMsgCrypt pc = new WXBizMsgCrypt(wechatService.getWechatToken(), wechatService.getAesKey(), wechatService.getAppId());
                     String decodedXml = pc.decryptMsg(messageRequest.getMsgSignature(), messageRequest.getTimestamp().toString(), messageRequest.getNonce(), fromXML);
                     System.out.println("decrypted: " + decodedXml);

                     TextMessage textMessage = MessageUtils.parseTextMessage(decodedXml);
                     System.out.println(textMessage);

                     String encryptedReply = pc.encryptMsg("success", messageRequest.getTimestamp().toString(), messageRequest.getNonce());
                     System.out.println("replay: " + encryptedReply);
                     ctx.end(encryptedReply);
                 } catch (Exception e) {
                     e.printStackTrace();
                     ctx.end("success");
                 }
             } else {
                 ctx.end("success");
             }
         })
         .listen("localhost", 8080);
    }
}
