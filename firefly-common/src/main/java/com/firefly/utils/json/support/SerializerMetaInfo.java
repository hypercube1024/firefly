package com.firefly.utils.json.support;

import static com.firefly.utils.json.JsonStringSymbol.OBJ_SEPARATOR;
import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.firefly.utils.json.Serializer;

public class SerializerMetaInfo implements Comparable<SerializerMetaInfo>{
	private char[] propertyName;
	private String propertyNameString;
	private Serializer serializer;
	private Method method;

	public String getPropertyNameString() {
		return propertyNameString;
	}

	public char[] getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName, boolean first) {
		propertyNameString = propertyName;
		this.propertyName = ((first ? "" : ",") + QUOTE + propertyName + QUOTE + OBJ_SEPARATOR).toCharArray();
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public Serializer getSerializer() {
		return serializer;
	}
	
	public void setMethod(Method method) {
		this.method = method;
	}

	public void toJson(Object obj, JsonStringWriter writer)
			throws IOException {
		try {
			Object ret = method.invoke(obj);
			if(ret == null) {
				writer.writeNull();
				return;
			}
			serializer.convertTo(writer, ret) ;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int compareTo(SerializerMetaInfo o) {
		return propertyNameString.compareTo(o.getPropertyNameString());
	}

}
