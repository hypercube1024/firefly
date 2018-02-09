package com.firefly.wechat.service.impl;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.HexUtils;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Action3;
import com.firefly.wechat.model.CommonRequest;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.*;
import com.firefly.wechat.service.WechatMessageService;
import com.firefly.wechat.utils.CtxUtils;
import com.firefly.wechat.utils.MessageXmlUtils;
import com.firefly.wechat.utils.WXBizMsgCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class WechatMessageServiceImpl implements WechatMessageService {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private String wechatToken;
    private String aesKey;
    private String appId;

    private List<Action3<MessageRequest, TextMessage, RoutingContext>> textMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, ImageMessage, RoutingContext>> imageMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, VoiceMessage, RoutingContext>> voiceMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, VideoMessage, RoutingContext>> videoMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, LocationMessage, RoutingContext>> locationMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, LinkMessage, RoutingContext>> linkMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, CommonMessage, RoutingContext>> subscribedMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, CommonMessage, RoutingContext>> unsubscribedMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, CommonMessage, RoutingContext>> scanMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, CommonMessage, RoutingContext>> clickMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, CommonMessage, RoutingContext>> viewMessageListeners = new LinkedList<>();
    private List<Action3<MessageRequest, ReportLocationMessage, RoutingContext>> reportMessageListeners = new LinkedList<>();
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
            if (echoListeners.isEmpty()) {
                EchoRequest req = (EchoRequest) commonRequest;
                if (verifyEchoString(req)) {
                    ctx.end(req.getEchostr());
                } else {
                    ctx.end("success");
                }
            } else {
                echoListeners.forEach(e -> e.call((EchoRequest) commonRequest, ctx));
            }
        } else if (commonRequest instanceof MessageRequest) {
            MessageRequest msgReq = (MessageRequest) commonRequest;
            log.info("received message request -> {}", msgReq.toString());

            String fromXML = ctx.getStringBody();
            try {
                WXBizMsgCrypt pc = new WXBizMsgCrypt(getWechatToken(), getAesKey(), getAppId());
                String decryptedXml = pc.decryptMsg(msgReq.getMsgSignature(), msgReq.getTimestamp().toString(), msgReq.getNonce(), fromXML);
                log.info("received message -> {}", decryptedXml);

                CommonMessage commonMessage = MessageXmlUtils.parseXml(decryptedXml, CommonMessage.class);
                Optional.ofNullable(commonMessage).map(CommonMessage::getMsgType).ifPresent(msgType -> {
                    switch (msgType) {
                        case "text": {
                            if (textMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                TextMessage textMessage = MessageXmlUtils.parseXml(decryptedXml, TextMessage.class);
                                textMessageListeners.forEach(e -> e.call(msgReq, textMessage, ctx));
                            }
                        }
                        break;
                        case "image": {
                            if (imageMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                ImageMessage imageMessage = MessageXmlUtils.parseXml(decryptedXml, ImageMessage.class);
                                imageMessageListeners.forEach(e -> e.call(msgReq, imageMessage, ctx));
                            }
                        }
                        break;
                        case "voice": {
                            if (voiceMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                VoiceMessage voiceMessage = MessageXmlUtils.parseXml(decryptedXml, VoiceMessage.class);
                                voiceMessageListeners.forEach(e -> e.call(msgReq, voiceMessage, ctx));
                            }
                        }
                        break;
                        case "video": {
                            if (videoMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                VideoMessage videoMessage = MessageXmlUtils.parseXml(decryptedXml, VideoMessage.class);
                                videoMessageListeners.forEach(e -> e.call(msgReq, videoMessage, ctx));
                            }
                        }
                        break;
                        case "location": {
                            if (locationMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                LocationMessage locationMessage = MessageXmlUtils.parseXml(decryptedXml, LocationMessage.class);
                                locationMessageListeners.forEach(e -> e.call(msgReq, locationMessage, ctx));
                            }
                        }
                        break;
                        case "link": {
                            if (linkMessageListeners.isEmpty()) {
                                defaultReplyMessage(ctx, msgReq);
                            } else {
                                LinkMessage linkMessage = MessageXmlUtils.parseXml(decryptedXml, LinkMessage.class);
                                linkMessageListeners.forEach(e -> e.call(msgReq, linkMessage, ctx));
                            }
                        }
                        break;
                        case "event": {
                            switch (commonMessage.getEvent()) {
                                case "subscribe": {
                                    if (subscribedMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        subscribedMessageListeners.forEach(e -> e.call(msgReq, commonMessage, ctx));
                                    }
                                }
                                break;
                                case "unsubscribe": {
                                    if (unsubscribedMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        unsubscribedMessageListeners.forEach(e -> e.call(msgReq, commonMessage, ctx));
                                    }
                                }
                                break;
                                case "SCAN": {
                                    if (scanMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        scanMessageListeners.forEach(e -> e.call(msgReq, commonMessage, ctx));
                                    }
                                }
                                break;
                                case "CLICK": {
                                    if (clickMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        clickMessageListeners.forEach(e -> e.call(msgReq, commonMessage, ctx));
                                    }
                                }
                                break;
                                case "VIEW": {
                                    if (viewMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        viewMessageListeners.forEach(e -> e.call(msgReq, commonMessage, ctx));
                                    }
                                }
                                break;
                                case "LOCATION": {
                                    if (reportMessageListeners.isEmpty()) {
                                        defaultReplyMessage(ctx, msgReq);
                                    } else {
                                        ReportLocationMessage reportLocationMessage = MessageXmlUtils.parseXml(decryptedXml, ReportLocationMessage.class);
                                        reportMessageListeners.forEach(e -> e.call(msgReq, reportLocationMessage, ctx));
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                });
            } catch (Exception e) {
                log.error("decrypt message exception", e);
            }
        } else {
            if (otherRequestListeners.isEmpty()) {
                ctx.end("success");
            } else {
                otherRequestListeners.forEach(e -> e.call(ctx));
            }
        }
    }

    protected void defaultReplyMessage(RoutingContext ctx, MessageRequest msgReq) {
        String replyXml = encryptMessage("success", System.currentTimeMillis() / 1000, msgReq.getNonce());
        log.info("reply message -> {}", replyXml);
        ctx.end(replyXml);
    }

    @Override
    public void addTextMessageListener(Action3<MessageRequest, TextMessage, RoutingContext> action) {
        textMessageListeners.add(action);
    }

    @Override
    public void addImageMessageListener(Action3<MessageRequest, ImageMessage, RoutingContext> action) {
        imageMessageListeners.add(action);
    }

    @Override
    public void addVoiceMessageListener(Action3<MessageRequest, VoiceMessage, RoutingContext> action) {
        voiceMessageListeners.add(action);
    }

    @Override
    public void addVideoMessageListener(Action3<MessageRequest, VideoMessage, RoutingContext> action) {
        videoMessageListeners.add(action);
    }

    @Override
    public void addLocationMessageListener(Action3<MessageRequest, LocationMessage, RoutingContext> action) {
        locationMessageListeners.add(action);
    }

    @Override
    public void addLinkMessageListener(Action3<MessageRequest, LinkMessage, RoutingContext> action) {
        linkMessageListeners.add(action);
    }

    @Override
    public void addSubscribedMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action) {
        subscribedMessageListeners.add(action);
    }

    @Override
    public void addUnsubscribedMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action) {
        unsubscribedMessageListeners.add(action);
    }

    @Override
    public void addScanMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action) {
        scanMessageListeners.add(action);
    }

    @Override
    public void addClickMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action) {
        clickMessageListeners.add(action);
    }

    @Override
    public void addViewMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action) {
        viewMessageListeners.add(action);
    }

    @Override
    public void addReportLocationMessageListener(Action3<MessageRequest, ReportLocationMessage, RoutingContext> action) {
        reportMessageListeners.add(action);
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
