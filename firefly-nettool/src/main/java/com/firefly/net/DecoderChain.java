package com.firefly.net;

abstract public class DecoderChain implements Decoder {

	protected volatile DecoderChain next;
	
	public DecoderChain() {}
	
	public DecoderChain(DecoderChain next) {
		this.next = next;
	}

	public DecoderChain getNext() {
		return next;
	}

	public void setNext(DecoderChain next) {
		this.next = next;
	}

}
