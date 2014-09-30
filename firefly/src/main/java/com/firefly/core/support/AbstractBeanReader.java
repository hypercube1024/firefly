package com.firefly.core.support;

import java.util.List;

import com.firefly.core.support.exception.BeanDefinitionParsingException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class AbstractBeanReader implements BeanReader {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	protected List<BeanDefinition> beanDefinitions;

	@Override
	public List<BeanDefinition> loadBeanDefinitions() {
		return beanDefinitions;
	}

	protected void error(String msg) {
		log.error(msg);
		throw new BeanDefinitionParsingException(msg);
	}
}
