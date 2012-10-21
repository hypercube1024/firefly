package com.firefly.mvc.web;

import com.firefly.core.ApplicationContext;
import com.firefly.mvc.web.Resource.Result;

public interface WebContext extends ApplicationContext {

	String getViewPath();

	String getEncoding();
	
	public Result match(String uri);
}
