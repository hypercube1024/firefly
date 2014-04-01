package com.firefly.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firefly.annotation.Inject;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanReader;
import com.firefly.core.support.xml.ManagedArray;
import com.firefly.core.support.xml.ManagedList;
import com.firefly.core.support.xml.ManagedMap;
import com.firefly.core.support.xml.ManagedRef;
import com.firefly.core.support.xml.ManagedValue;
import com.firefly.core.support.xml.XmlBeanDefinition;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.utils.ConvertUtils;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.BeanMethodFilter;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.VerifyUtils;

/**
 * 
 * @author Xujj, Alvin Qiu
 * 
 */
public class XmlApplicationContext extends AbstractApplicationContext {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public XmlApplicationContext() {
		this(null);
	}

	public XmlApplicationContext(String file) {
		super(file);
	}

	@Override
	protected List<BeanDefinition> getBeanDefinitions(String file) {
		List<BeanDefinition> list1 = new AnnotationBeanReader(file)
				.loadBeanDefinitions();
		List<BeanDefinition> list2 = new XmlBeanReader(file)
				.loadBeanDefinitions();
		if (list1 != null && list2 != null) {
			log.debug("mixed bean");
			list1.addAll(list2);
			return list1;
		} else if (list1 != null) {
			log.debug("annotation bean");
			return list1;
		} else if (list2 != null) {
			log.debug("xml bean");
			return list2;
		}
		return null;
	}

	@Override
	protected Object inject(BeanDefinition beanDef) {
		if (beanDef instanceof XmlBeanDefinition)
			return xmlInject(beanDef);
		else if (beanDef instanceof AnnotationBeanDefinition)
			return annotationInject(beanDef);
		else
			return null;
	}

	private Object xmlInject(BeanDefinition beanDef) {
		XmlBeanDefinition beanDefinition = (XmlBeanDefinition) beanDef;
		final Object object = beanDefinition.getObject();
		final Map<String, Object> properties = beanDefinition.getProperties();

		Class<?> clazz = object.getClass();
		ReflectUtils.getSetterMethods(clazz, new BeanMethodFilter(){

			@Override
			public boolean accept(String propertyName, Method method) {
				Object value = properties.get(propertyName);
				if (value != null) {
					try {
						method.invoke(object, getInjectArg(value, method));
					} catch (Throwable t) {
						log.error("xml inject error", t);
					}
				}
				return false;
			}});

		addObjectToContext(beanDefinition);
		return object;
	}

	private Object getInjectArg(Object value, Method method) {
		if (value instanceof ManagedValue) { // value
			return getValueArg(value, method);
		} else if (value instanceof ManagedRef) { // ref
			return getRefArg(value, method);
		} else if (value instanceof ManagedList) { // list
			return getListArg(value, method);
		} else if (value instanceof ManagedArray) { // array
			return getArrayArg(value, method);
		} else if (value instanceof ManagedMap) { // map
			return getMapArg(value, method);
		} else
			return null;
	}

	private Object getValueArg(Object value, Method method) {
		ManagedValue managedValue = (ManagedValue) value;
		String typeName = null;
		if (method == null) {
			typeName = VerifyUtils.isEmpty(managedValue.getTypeName()) ? null
					: managedValue.getTypeName();
		} else {
			typeName = VerifyUtils.isEmpty(managedValue.getTypeName()) ? method
					.getParameterTypes()[0].getName() : managedValue
					.getTypeName();
		}

		log.debug("value type [{}]", typeName);
		return ConvertUtils.convert(managedValue.getValue(), typeName);
	}

	private Object getRefArg(Object value, Method method) {
		ManagedRef ref = (ManagedRef) value;
		Object instance = map.get(ref.getBeanName());
		if (instance == null) {
			BeanDefinition b = findBeanDefinition(ref.getBeanName());
			if (b != null)
				instance = inject(b);
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private Object getListArg(Object value, Method method) {
		Class<?> setterParamType = null;
		if (method != null) {
			setterParamType = method.getParameterTypes()[0];
		}
		ManagedList<Object> values = (ManagedList<Object>) value;
		Collection<Object> collection = null;

		if (VerifyUtils.isNotEmpty(values.getTypeName())) { // 指定了list的类型
			try {
				collection = (Collection<Object>) XmlApplicationContext.class
						.getClassLoader().loadClass(values.getTypeName())
						.newInstance();
			} catch (Throwable t) {
				log.error("list inject error", t);
			}
		} else {
			collection = (setterParamType == null ? new ArrayList<Object>()
					: ConvertUtils.getCollectionObj(setterParamType));
		}

		for (Object item : values) {
			Object listValue = getInjectArg(item, null);
			collection.add(listValue);
		}

		return collection;
	}

	@SuppressWarnings("unchecked")
	private Object getArrayArg(Object value, Method method) {
		Class<?> setterParamType = null;
		if (method != null) {
			setterParamType = method.getParameterTypes()[0];
		}
		ManagedArray<Object> values = (ManagedArray<Object>) value;
		Collection<Object> collection = new ArrayList<Object>();
		for (Object item : values) {
			Object listValue = getInjectArg(item, null);
			collection.add(listValue);
		}
		return ConvertUtils.convert(collection, setterParamType);
	}


	@SuppressWarnings("unchecked")
	private Object getMapArg(Object value, Method method) {
		Class<?> setterParamType = null;
		if (method != null) {
			setterParamType = method.getParameterTypes()[0];
		}
		ManagedMap<Object, Object> values = (ManagedMap<Object, Object>) value;
		Map<Object, Object> m = null;
		if (VerifyUtils.isNotEmpty(values.getTypeName())) {
			try {
				m = (Map<Object, Object>) XmlApplicationContext.class.getClassLoader()
						.loadClass(values.getTypeName())
						.newInstance();
			} catch (Throwable t) {
				log.error("map inject error", t);
			}
		} else { 
			m = (setterParamType == null ? new HashMap<Object, Object>() : ConvertUtils.getMapObj(setterParamType));
			log.debug("map ret [{}]", m.getClass().getName());
		}
		for (Object o : values.keySet()) {
			Object k = getInjectArg(o, null);
			Object v = getInjectArg(values.get(o), null);
			m.put(k, v);
		}
		return m;
	}

	/**
	 * annotation injecting
	 * 
	 * @param beanDef
	 * @return
	 */
	private Object annotationInject(BeanDefinition beanDef) {
		AnnotationBeanDefinition beanDefinition = (AnnotationBeanDefinition) beanDef;
		fieldInject(beanDefinition);
		methodInject(beanDefinition);
		addObjectToContext(beanDefinition);
		return beanDefinition.getObject();
	}

	private void fieldInject(AnnotationBeanDefinition beanDefinition) {
		Object object = beanDefinition.getObject();

		for (Field field : beanDefinition.getInjectFields()) {
			field.setAccessible(true);
			Class<?> clazz = field.getType();
			String id = field.getAnnotation(Inject.class).value();
			String key = VerifyUtils.isNotEmpty(id) ? id : clazz.getName();
			Object instance = map.get(key);
			if (instance == null) {
				BeanDefinition b = findBeanDefinition(key);
				if (b != null)
					instance = inject(b);
			}
			if (instance != null) {
				try {
					field.set(object, instance);
				} catch (Throwable t) {
					log.error("field inject error", t);
				}
			}
		}
	}

	private void methodInject(AnnotationBeanDefinition beanDefinition) {
		Object object = beanDefinition.getObject();

		for (Method method : beanDefinition.getInjectMethods()) {
			method.setAccessible(true);
			Class<?>[] params = method.getParameterTypes();
			Object[] p = new Object[params.length];
			for (int i = 0; i < p.length; i++) {
				String key = params[i].getName();
				Object instance = map.get(key);
				if (instance != null) {
					p[i] = instance;
				} else {
					BeanDefinition b = findBeanDefinition(key);
					if (b != null)
						p[i] = inject(b);
				}
			}
			try {
				method.invoke(object, p);
			} catch (Throwable t) {
				log.error("method inject error", t);
			}
		}
	}
}
