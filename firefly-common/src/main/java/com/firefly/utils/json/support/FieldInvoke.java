package com.firefly.utils.json.support;

import java.lang.reflect.Field;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyField;

public class FieldInvoke implements PropertyInvoke {
	
	private ProxyField field;

	public FieldInvoke(Field field) {
		try {
			this.field = ReflectUtils.getProxyField(field);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object get(Object obj) {
		return field.get(obj);
	}

	@Override
	public void set(Object obj, Object arg) {
		field.set(obj, arg);
	}

}
