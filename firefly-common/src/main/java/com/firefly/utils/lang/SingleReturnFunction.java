package com.firefly.utils.lang;

public interface SingleReturnFunction<F, T> {
	F apply(T input);
}
