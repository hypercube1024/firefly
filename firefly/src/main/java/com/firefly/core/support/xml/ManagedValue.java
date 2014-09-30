package com.firefly.core.support.xml;

/**
 * Literal value node
 * @author JJ Xu
 * @date 2011-3-9
 */
public class ManagedValue implements XmlManagedNode {

	private String value;
	private String typeName;
	
	public ManagedValue() {
	}
	
	public ManagedValue(String value) {
		this(value,null);
	}
	
	public ManagedValue(String value,String typeName) {
		this.value = value;
		this.typeName = typeName;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}
