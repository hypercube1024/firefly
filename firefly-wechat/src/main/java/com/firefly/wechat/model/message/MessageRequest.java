package com.firefly.wechat.model.message;

import com.firefly.wechat.model.CommonRequest;

/**
 * @author Pengtao Qiu
 */
public class MessageRequest extends CommonRequest {

    protected String msgSignature;
    protected String encryptType;

    public String getMsgSignature() {
        return msgSignature;
    }

    public void setMsgSignature(String msgSignature) {
        this.msgSignature = msgSignature;
    }

    public String getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(String encryptType) {
        this.encryptType = encryptType;
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "msgSignature='" + msgSignature + '\'' +
                ", encryptType='" + encryptType + '\'' +
                ", signature='" + signature + '\'' +
                ", nonce='" + nonce + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
