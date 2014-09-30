package com.firefly.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.exception.BeanDefinitionParsingException;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class AbstractApplicationContext implements ApplicationContext {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	protected Map<String, Object> map = new HashMap<String, Object>();
	protected Set<String> errorMemo = new HashSet<String>();
	protected List<BeanDefinition> beanDefinitions;

	public AbstractApplicationContext() {
		this(null);
	}

	public AbstractApplicationContext(String file) {
		beanDefinitions = getBeanDefinitions(file);
		check(); //Conflicts check
		addObjectToContext();
	}

	private void addObjectToContext() {
		for (BeanDefinition beanDefinition : beanDefinitions) {
			inject(beanDefinition);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> clazz) {
		return (T) map.get(clazz.getName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(String id) {
		return (T) map.get(id);
	}

	/**
	 * Conflicts check
	 * 1.More than two components have the same id
	 * 2.The one of two components that have the same class name or interface name dosen't set id.
	 * 3.Two components have the same class name or interface name and different id, save memo. 
	 *   When the component is injecting by type, throw a exception. 
	 * 
	 */
	protected void check() {
		for (int i = 0; i < beanDefinitions.size(); i++) {
			for (int j = i + 1; j < beanDefinitions.size(); j++) {
				log.debug("check bean " + i + "|" + j);
				BeanDefinition b1 = beanDefinitions.get(i);
				BeanDefinition b2 = beanDefinitions.get(j);

				if (VerifyUtils.isNotEmpty(b1.getId())
						&& VerifyUtils.isNotEmpty(b2.getId())
						&& b1.getId().equals(b2.getId())) {
					error("bean " + b1.getClassName() + " and bean "
							+ b2.getClassName() + " have duplicate id ");
				}

				if (b1.getClassName().equals(b2.getClassName())) {
					if (VerifyUtils.isEmpty(b1.getId())
							|| VerifyUtils.isEmpty(b2.getId())) {
						error("class " + b1.getClassName()
								+ " duplicate definition");
					} else {
						errorMemo.add(b1.getClassName());
					}
				}

				for (String iname1 : b1.getInterfaceNames()) {
					for (String iname2 : b2.getInterfaceNames()) {
						if (iname1.equals(iname2)) {
							if (VerifyUtils.isEmpty(b1.getId())
									|| VerifyUtils.isEmpty(b2.getId())) {
								error("class " + b1.getClassName()
										+ " duplicate definition");
							} else {
								errorMemo.add(iname1);
							}
						}
					}
				}

			}
		}
	}

	protected void check(String key) {
		if(errorMemo.contains(key)) {
			error(key + " auto inject failure!");
		}
	}

	protected void addObjectToContext(final BeanDefinition beanDefinition, final Object object) {
		// context key using id
		String id = beanDefinition.getId();
		if (VerifyUtils.isNotEmpty(id))
			map.put(id, object);

		// context key using class name
		map.put(beanDefinition.getClassName(), object);

		// context key using interface name
		String[] keys = beanDefinition.getInterfaceNames();
		for (String k : keys) {
			map.put(k, object);
		}
	}

	protected BeanDefinition findBeanDefinition(String key) {
		check(key);
		BeanDefinition ret = null;
		for (BeanDefinition beanDefinition : beanDefinitions) {
			if (key.equals(beanDefinition.getId())) {
				ret = beanDefinition;
				break;
			} else if (key.equals(beanDefinition.getClassName())) {
				ret = beanDefinition;
				break;
			} else {
				for (String interfaceName : beanDefinition.getInterfaceNames()) {
					if (key.equals(interfaceName)) {
						ret = beanDefinition;
						break;
					}
				}
			}
		}
		return ret;
	}

	protected void error(String msg) {
		log.error(msg);
		throw new BeanDefinitionParsingException(msg);
	}

	abstract protected List<BeanDefinition> getBeanDefinitions(String file);

	abstract protected Object inject(BeanDefinition beanDef);

}
