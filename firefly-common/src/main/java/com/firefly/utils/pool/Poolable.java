package com.firefly.utils.pool;

public interface Poolable {
	void release();
	void prepare();
}
