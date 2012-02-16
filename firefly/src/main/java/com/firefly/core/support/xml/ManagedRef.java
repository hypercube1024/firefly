package com.firefly.core.support.xml;

/**
 * ref元素
 * @author 须俊杰
 * @date 2011-3-9
 */
public class ManagedRef {

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
