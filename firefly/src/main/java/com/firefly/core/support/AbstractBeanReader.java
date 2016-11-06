package com.firefly.core.support;

import com.firefly.core.support.exception.BeanDefinitionParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AbstractBeanReader implements BeanReader {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
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
