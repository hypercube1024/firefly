package com.firefly.wechat.model;

/**
 * @author Pengtao Qiu
 */
public class EchoRequest extends CommonRequest {

    private String echostr;

    public String getEchostr() {
        return echostr;
    }

    public void setEchostr(String echostr) {
        this.echostr = echostr;
    }

    @Override
    public String toString() {
        return "EchoRequest{" +
                "echostr='" + echostr + '\'' +
                ", signature='" + signature + '\'' +
                ", nonce='" + nonce + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
