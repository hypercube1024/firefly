package com.firefly.utils.lang;

public interface MultiReturnFunction<F, S, T> {
	Pair<F, S> apply(T input);
}
