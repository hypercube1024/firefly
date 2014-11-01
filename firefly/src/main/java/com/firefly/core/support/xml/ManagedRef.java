package com.firefly.core.support.xml;

/**
 * Reference elements
 * @author JJ Xu
 */
public class ManagedRef implements XmlManagedNode {

	private String beanName;

	public ManagedRef() {
	}
	
	public ManagedRef(String beanName) {
		this.beanName = beanName;
	}
	
	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
}
