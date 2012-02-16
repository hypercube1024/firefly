package com.firefly.core.support.exception;

/**
 * 解析BeanDefinition异常
 * @author 杰然不同
 * @date 2011-3-8
 */
@SuppressWarnings("serial")
public class BeanDefinitionParsingException extends BeansException {

	public BeanDefinitionParsingException(String msg) {
		super(msg);
	}

}
