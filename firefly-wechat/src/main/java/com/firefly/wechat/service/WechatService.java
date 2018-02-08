package com.firefly.wechat.service;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Action3;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.ImageMessage;
import com.firefly.wechat.model.message.MessageRequest;
import com.firefly.wechat.model.message.TextMessage;

/**
 * @author Pengtao Qiu
 */
public interface WechatService {

    boolean verifyEchoString(EchoRequest request);

    String encryptMessage(String reply, Long timeStamp, String nonce);

    String getWechatToken();

    String getAesKey();

    String getAppId();

    void onRequest(RoutingContext ctx);

    void addTextMessageListener(Action3<MessageRequest, TextMessage, RoutingContext> action);

    void addImageMessageListener(Action3<MessageRequest, ImageMessage, RoutingContext> action);

    void addEchoStringListener(Action2<EchoRequest, RoutingContext> action);

    void addOtherRequestListener(Action1<RoutingContext> action);

}
