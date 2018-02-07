package com.firefly.wechat.service.impl;

import com.firefly.utils.Assert;
import com.firefly.utils.codec.HexUtils;
import com.firefly.wechat.model.EchoRequest;
import com.firefly.wechat.service.WechatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * @author Pengtao Qiu
 */
public class WechatServiceImpl implements WechatService {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private String wechatToken;

    public String getWechatToken() {
        return wechatToken;
    }

    public void setWechatToken(String wechatToken) {
        this.wechatToken = wechatToken;
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
}
