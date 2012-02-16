package com.firefly.mvc.web;

import com.firefly.core.ApplicationContext;

public interface WebContext extends ApplicationContext {

	String getViewPath();

	String getEncoding();
}
