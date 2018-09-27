package com.firefly.core.support.xml;

import java.util.List;
import java.util.Map;

import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanDefinition;

/**
 * XML bean configuration
 *
 * @author JJ Xu
 */
public interface XmlBeanDefinition extends AnnotationBeanDefinition {

    Map<String, XmlManagedNode> getProperties();

    void setProperties(Map<String, XmlManagedNode> properties);

    void setContructorParameters(List<XmlManagedNode> list);

    List<XmlManagedNode> getContructorParameters();

}
