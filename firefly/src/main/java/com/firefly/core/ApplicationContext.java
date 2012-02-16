package com.firefly.core;

public interface ApplicationContext {

	<T> T getBean(Class<T> clazz);

	<T> T getBean(String id);
}
