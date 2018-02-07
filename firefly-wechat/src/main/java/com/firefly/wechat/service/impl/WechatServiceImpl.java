package com.firefly.wechat.service.impl;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.HexUtils;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Action3;
import com.firefly.wechat.model.CommonRequest;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.CommonMessage;
import com.firefly.wechat.model.message.MessageRequest;
import com.firefly.wechat.model.message.TextMessage;
import com.firefly.wechat.service.WechatService;
import com.firefly.wechat.utils.CtxUtils;
import com.firefly.wechat.utils.MessageUtils;
import com.firefly.wechat.utils.WXBizMsgCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 * @author Pengtao Qiu
 */
public class WechatServiceImpl implements WechatService {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private String wechatToken;
    private String aesKey;
    private String appId;

    private List<Action3<MessageRequest, TextMessage, RoutingContext>> textMessageListeners = new LinkedList<>();
    private List<Action2<EchoRequest, RoutingContext>> echoListeners = new LinkedList<>();
    private List<Action1<RoutingContext>> otherRequestListeners = new LinkedList<>();

    public String getWechatToken() {
        return wechatToken;
    }

    public void setWechatToken(String wechatToken) {
        this.wechatToken = wechatToken;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public void onRequest(RoutingContext ctx) {
        CommonRequest commonRequest = CtxUtils.toRequest(ctx);
        if (commonRequest instanceof EchoRequest) {
            echoListeners.forEach(e -> e.call((EchoRequest) commonRequest, ctx));
        } else if (commonRequest instanceof MessageRequest) {
            MessageRequest messageRequest = (MessageRequest) commonRequest;

            String fromXML = ctx.getStringBody();
            try {
                WXBizMsgCrypt pc = new WXBizMsgCrypt(getWechatToken(), getAesKey(), getAppId());
                String decryptedXml = pc.decryptMsg(messageRequest.getMsgSignature(), messageRequest.getTimestamp().toString(), messageRequest.getNonce(), fromXML);
                log.info("received message -> {}", decryptedXml);

                CommonMessage commonMessage = MessageUtils.parseCommonMessage(decryptedXml);
                switch (commonMessage.getMsgType()) {
                    case "text": {
                        TextMessage textMessage = MessageUtils.parseTextMessage(decryptedXml);
                        textMessageListeners.forEach(e -> e.call(messageRequest, textMessage, ctx));
                    }
                    break;
                    case "image": {

                    }
                    break;
                    case "voice": {

                    }
                    break;
                    case "video": {

                    }
                    break;
                    case "shortvideo": {

                    }
                    break;
                    case "location": {

                    }
                    break;
                    case "link": {

                    }
                    break;
                }
            } catch (Exception e) {
                log.error("decrypt message exception", e);
            }
        } else {
            otherRequestListeners.forEach(e -> e.call(ctx));
        }
    }

    @Override
    public void addTextMessageListener(Action3<MessageRequest, TextMessage, RoutingContext> action) {
        textMessageListeners.add(action);
    }

    @Override
    public void addEchoStringListener(Action2<EchoRequest, RoutingContext> action) {
        echoListeners.add(action);
    }

    @Override
    public void addOtherRequestListener(Action1<RoutingContext> action) {
        otherRequestListeners.add(action);
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public boolean verifyEchoString(EchoRequest request) {
        Assert.hasText(request.getEchostr(), "The echo string must be not empty");
        Assert.hasText(request.getSignature(), "The signature string must be not empty");
        Assert.hasText(request.getNonce(), "The nonce string must be not empty");
        Assert.notNull(request.getTimestamp(), "The timestamp must be not null");
        Assert.isTrue(request.getTimestamp() > 0, "The timestamp string must be greater than 0");

        TreeSet<String> set = new TreeSet<>(Arrays.asList(request.getNonce(), request.getTimestamp().toString(), wechatToken));
        StringBuilder sign = new StringBuilder();
        set.forEach(sign::append);
        try {
            String originSign = sign.toString();
            String hexSign = HexUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(originSign.getBytes(StandardCharsets.US_ASCII)));
            log.info("verify echo string. {} | {} | {}", originSign, hexSign, request.getSignature());
            return request.getSignature().equals(hexSign);
        } catch (Exception e) {
            log.error("verify echo string exception", e);
            return false;
        }
    }

    @Override
    public String encryptMessage(String reply, Long timeStamp, String nonce) {
        try {
            WXBizMsgCrypt pc = new WXBizMsgCrypt(getWechatToken(), getAesKey(), getAppId());
            return pc.encryptMsg(reply, timeStamp.toString(), nonce);
        } catch (Exception e) {
            log.error("encrypt message exception", e);
            return null;
        }
    }
}
