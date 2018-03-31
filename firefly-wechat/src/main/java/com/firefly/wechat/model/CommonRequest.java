package com.firefly.wechat.model;

/**
 * @author Pengtao Qiu
 */
public class CommonRequest {

    protected String signature;
    protected String nonce;
    protected Long timestamp;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CommonRequest{" +
                "signature='" + signature + '\'' +
                ", nonce='" + nonce + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
