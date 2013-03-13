package com.firefly.utils.json.support;

import java.lang.reflect.Field;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.FieldProxy;

public class FieldInvoke implements PropertyInvoke {
	
	private FieldProxy field;

	public FieldInvoke(Field field) {
		try {
			this.field = ReflectUtils.getFieldProxy(field);
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
