package com.fireflysource.demo.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Interceptor(uri = "/goods/*", order=0)
public class GoodsInformationInterceptorStep1 {

	private static Log log = LogFactory.getInstance().getLog("demo-log");
	
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		log.info("befor goods information step 1");
		View view = chain.doNext(request, response, chain);
		log.info("after goods information step 1");
		return view;
	}
}
