package com.firefly.wechat.service;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Action3;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.*;

/**
 * @author Pengtao Qiu
 */
public interface WechatMessageService {

    boolean verifyEchoString(EchoRequest request);

    String encryptMessage(String reply, Long timeStamp, String nonce);

    String getWechatToken();

    String getAesKey();

    String getAppId();

    void onRequest(RoutingContext ctx);

    void addTextMessageListener(Action3<MessageRequest, TextMessage, RoutingContext> action);

    void addImageMessageListener(Action3<MessageRequest, ImageMessage, RoutingContext> action);

    void addVoiceMessageListener(Action3<MessageRequest, VoiceMessage, RoutingContext> action);

    void addVideoMessageListener(Action3<MessageRequest, VideoMessage, RoutingContext> action);

    void addLocationMessageListener(Action3<MessageRequest, LocationMessage, RoutingContext> action);

    void addLinkMessageListener(Action3<MessageRequest, LinkMessage, RoutingContext> action);

    void addSubscribedMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action);

    void addUnsubscribedMessageListener(Action3<MessageRequest, CommonMessage, RoutingContext> action);

    void addEchoStringListener(Action2<EchoRequest, RoutingContext> action);

    void addOtherRequestListener(Action1<RoutingContext> action);

}
