package com.firefly.core.support.xml.parse;

import com.firefly.core.support.exception.BeanDefinitionParsingException;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class AbstractXmlNodeParser implements XmlNodeParser {

	protected static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected void error(String msg) {
		log.error(msg);
		throw new BeanDefinitionParsingException(msg);
	}
}
