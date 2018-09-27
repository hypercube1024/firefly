package com.firefly.core.support.xml.parse;

import com.firefly.core.support.exception.BeanDefinitionParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXmlNodeParser implements XmlNodeParser {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected void error(String msg) {
        log.error(msg);
        throw new BeanDefinitionParsingException(msg);
    }
}
