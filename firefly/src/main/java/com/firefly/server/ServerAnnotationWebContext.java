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

public class ServerAnnotationWebContext extends AnnotationWebContext {
	
	private final Config serverConfig;
	/**
	 * 用于firefly http服务器
	 * 
	 * @param file
	 *            firefly配置文件
	 * @param serverHome
	 *            http服务根目录
	 */
	public ServerAnnotationWebContext(Config serverConfig) {
		super(serverConfig.getConfigFileName());
		TemplateView.init(new File(serverConfig.getServerHome(), getViewPath()).getAbsolutePath(), getEncoding());
		StaticFileView.init(serverConfig, getViewPath());
		this.serverConfig = serverConfig;
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
