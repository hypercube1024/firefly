package com.firefly.net;

abstract public class EncoderChain implements Encoder {

    protected volatile EncoderChain next;

    public EncoderChain() {
    }

    public EncoderChain(EncoderChain next) {
        this.next = next;
    }

    public EncoderChain getNext() {
        return next;
    }

    public void setNext(EncoderChain next) {
        this.next = next;
    }


}
