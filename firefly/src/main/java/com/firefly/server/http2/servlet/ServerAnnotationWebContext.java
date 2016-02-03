package com.firefly.server.http2.servlet;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.WebHandler;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.mvc.web.view.StaticFileView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerAnnotationWebContext extends AnnotationWebContext {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final HTTP2Configuration http2Configuration;

	public ServerAnnotationWebContext(String file) {
		super(file);
		http2Configuration = getBean(HTTP2Configuration.class);
		http2Configuration.setConfigFileName(file);
		viewInit();
	}

	public ServerAnnotationWebContext(HTTP2Configuration http2Configuration) {
		super(http2Configuration.getConfigFileName());
		this.http2Configuration = http2Configuration;
		viewInit();
	}

	private void viewInit() {
		SystemHtmlPage.addErrorPageMap(http2Configuration.getErrorPage());
		TemplateView.init(new File(http2Configuration.getServerHome(), getViewPath()).getAbsolutePath(), getEncoding());
		StaticFileView.init(http2Configuration.getCharacterEncoding(), http2Configuration.getFileAccessFilter(),
				http2Configuration.getServerHome(), http2Configuration.getMaxRangeNum(), getViewPath());
		this.http2Configuration.setCharacterEncoding(getEncoding());
		log.info("server home is {}", http2Configuration.getServerHome());
	}

	@Override
	protected void addLastHandler(String uri, String servletURI, final HandlerChainImpl chain) {
		WebHandler last = null;
		if (servletURI != null)
			last = resource.match(servletURI);

		if (last != null) {
			chain.add(last);
			return;
		}

		final String path = uri.equals("/") ? "/index.html" : uri;
		File file = new File(http2Configuration.getServerHome(), path);
		if (!file.exists() || file.isDirectory())
			return;

		chain.add(new WebHandler() {

			@Override
			public View invoke(HttpServletRequest request, HttpServletResponse response) {
				return new StaticFileView(path);
			}

		});
	}
}
