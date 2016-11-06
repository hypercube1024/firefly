package com.firefly.mvc.web.support;

import com.firefly.utils.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

public class ParamMetaInfo {
	private static Logger log = LoggerFactory.getLogger("firefly-system");

	// injecting type
	private final Class<?> paramClass;

	// setter method of parameter
	private final Map<String, Method> beanSetMethod;

	// out attribute
	private final String attribute;

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
	 * 
	 * @param o
	 *            The parameter object
	 * @param key
	 *            The field name of the parameter object
	 * @param value
	 *            The value
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
	 * 
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

	public Class<?> getParamClass() {
		return paramClass;
	}

}
