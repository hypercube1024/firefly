package com.firefly.net;

abstract public class DecoderChain implements Decoder {

    protected final DecoderChain next;

    public DecoderChain(DecoderChain next) {
        this.next = next;
    }

    public DecoderChain getNext() {
        return next;
    }

}
