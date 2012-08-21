package com.firefly.utils.json.support;

import java.lang.reflect.Field;

public class FieldInvoke implements PropertyInvoke {
	
	private Field field;

	public FieldInvoke(Field field) {
		this.field = field;
	}



	@Override
	public Object get(Object obj) {
		Object ret = null;
		try {
			ret = field.get(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return ret;
	}



	@Override
	public void set(Object obj, Object arg) {
		try {
			field.set(obj, arg);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
