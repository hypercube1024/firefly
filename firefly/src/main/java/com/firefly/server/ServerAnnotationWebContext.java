package com.firefly.server;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.WebHandler;
import com.firefly.mvc.web.view.StaticFileView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.server.http.Config;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerAnnotationWebContext extends AnnotationWebContext {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private final Config serverConfig;
	
	public ServerAnnotationWebContext(String file) {
		super(file);
		serverConfig = getBean(Config.class);
		serverConfig.setConfigFileName(file);
		viewInit();
	}
	
	public ServerAnnotationWebContext(Config serverConfig) {
		super(serverConfig.getConfigFileName());
		this.serverConfig = serverConfig;
		viewInit();
	}
	
	private void viewInit() {
		log.info("server config file [{}]", serverConfig.getConfigFileName());
		log.info("server home [{}]", serverConfig.getServerHome());
		log.info("context path [{}]", serverConfig.getContextPath());
		log.info("servlet path [{}]", serverConfig.getServletPath());
		log.info("host [{}:{}]", serverConfig.getHost(), serverConfig.getPort());
		TemplateView.init(new File(serverConfig.getServerHome(), getViewPath()).getAbsolutePath(), getEncoding());
		StaticFileView.init(serverConfig, getViewPath());
	}
	
	@Override
	protected void addLastHandler(String uri, String servletURI, final HandlerChainImpl chain) {
		WebHandler last = null;
		if(servletURI != null)
			last = resource.match(servletURI);
		
		if(last != null) {
			chain.add(last);
			return;
		} 
		
		final String path = uri.equals("/") ? "/index.html" : uri;
		File file = new File(serverConfig.getServerHome(), path);
		if (!file.exists() || file.isDirectory())
			return;
		
		chain.add(new WebHandler(){
			
			@Override
			public View invoke(HttpServletRequest request, HttpServletResponse response) {
				return new StaticFileView(path);
			}
			
		});
	}
}
