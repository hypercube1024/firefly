package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils.MethodProxy;

public interface ClassProxy {
	public Object intercept(MethodProxy handler, Object originalInstance, Object[] args);
}
