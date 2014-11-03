package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Map;

import com.firefly.utils.ConvertUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ParamMetaInfo {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final Class<?> paramClass; // injecting type
	private final Map<String, Method> beanSetMethod; // setter method of parameter
	private final String attribute; // out attribute

	public ParamMetaInfo(Class<?> paramClass, Map<String, Method> beanSetMethod, String attribute) {
		this.paramClass = paramClass;
		this.beanSetMethod = beanSetMethod;
		this.attribute = attribute;
	}

	public String getAttribute() {
		return attribute;
	}

	/**
	 * Set value of the parameter object
	 * @param o The parameter object
	 * @param key The field name of the parameter object
	 * @param value The value
	 */
	public void setParam(Object o, String key, String value) {
		try {
			Method m = beanSetMethod.get(key);
			if (m != null) {
				Class<?> p = m.getParameterTypes()[0];
				m.invoke(o, ConvertUtils.convert(value, p));
			}
		} catch (Throwable t) {
			log.error("set param error", t);
		}
	}

	/**
	 * Get an new instance of a parameter;
	 * @return The instance
	 */
	public Object newParamInstance() {
		Object o = null;
		try {
			o = paramClass.newInstance();
		} catch (Throwable t) {
			log.error("new param error", t);
		}
		return o;
	}

}
