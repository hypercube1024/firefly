package com.firefly.wechat.utils;

import com.firefly.$;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.wechat.model.CommonRequest;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.model.message.MessageRequest;

/**
 * @author Pengtao Qiu
 */
abstract public class CtxUtils {

    @SuppressWarnings("unchecked")
    public static <T extends CommonRequest> T toRequest(RoutingContext ctx) {
        if ($.string.hasText(ctx.getParameter("echostr"))) {
            EchoRequest request = new EchoRequest();
            request.setEchostr(ctx.getParameter("echostr"));
            request.setNonce(ctx.getParameter("nonce"));
            request.setSignature(ctx.getParameter("signature"));
            request.setTimestamp(ctx.getParamOpt("timestamp").map(Long::parseLong).orElse(0L));
            return (T) request;
        } else if ($.string.hasText(ctx.getParameter("msg_signature"))) {
            MessageRequest request = new MessageRequest();
            request.setNonce(ctx.getParameter("nonce"));
            request.setSignature(ctx.getParameter("signature"));
            request.setTimestamp(ctx.getParamOpt("timestamp").map(Long::parseLong).orElse(0L));
            request.setMsgSignature(ctx.getParameter("msg_signature"));
            request.setEncryptType(ctx.getParameter("encrypt_type"));
            return (T) request;
        } else {
            return null;
        }
    }
}
