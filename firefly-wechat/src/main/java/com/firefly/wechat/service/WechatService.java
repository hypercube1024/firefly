package com.firefly.wechat.service;

import com.firefly.wechat.model.EchoRequest;

/**
 * @author Pengtao Qiu
 */
public interface WechatService {

    boolean verifyEchoString(EchoRequest request);

    String getWechatToken();

    String getAesKey();

    String getAppId();

}
