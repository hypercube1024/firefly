package com.firefly.utils.lang;

public class Pair<F, S> {
	
	public F first;
	public S second;

	public Pair() {
	}

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}

}
