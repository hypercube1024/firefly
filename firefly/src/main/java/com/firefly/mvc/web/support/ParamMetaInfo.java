package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Map;
import com.firefly.utils.ConvertUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ParamMetaInfo {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final Class<?> paramClass; // 要注入的类型
	private final Map<String, Method> beanSetMethod; // 要注入的bean的set方法
	private final String attribute; // 要setAttribute的属性

	public ParamMetaInfo(Class<?> paramClass, Map<String, Method> beanSetMethod,
			String attribute) {
		super();
		this.paramClass = paramClass;
		this.beanSetMethod = beanSetMethod;
		this.attribute = attribute;
	}

	public String getAttribute() {
		return attribute;
	}

	/**
	 * 给参数对象的实例赋值
	 * @param o 要赋值的对象
	 * @param key 要赋值的属性
	 * @param value 要赋的值
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
	 * 新建一个参数对象实例
	 * @return
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
