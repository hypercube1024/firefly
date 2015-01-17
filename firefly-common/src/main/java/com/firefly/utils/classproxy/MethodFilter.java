package com.firefly.utils.classproxy;

import java.lang.reflect.Method;

public interface MethodFilter {
	public boolean accept(Method method);
}
