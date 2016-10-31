package com.firefly.mvc.web.view;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class TemplateView implements View {
	
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	protected static TemplateFactory templateFactory;
	protected static boolean init = false;
	protected static String charset = "UTF-8";
	protected static String _viewPath;
	
	protected String page;
	
	public static void init(String viewPath, String encoding) {
		if (!init) {
			log.info("the template path is {}", viewPath);
			_viewPath = viewPath;
			com.firefly.template.Config config = new com.firefly.template.Config();
			config.setViewPath(viewPath);
			config.setCharset(encoding);
			charset = encoding;
			templateFactory = new TemplateFactory(config).init();
			init = true;
		}
	}
	
	public static TemplateFactory getTemplateFactory() {
		return templateFactory;
	}
	
	public static String getCharset() {
		return charset;
	}
	
	public static String getViewPath() {
		return _viewPath;
	}
	
	public String getPage() {
		return page;
	}

	public TemplateView(String page) {
		this.page = page;
	}

	@Override
	public void render(final HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		com.firefly.template.View v = templateFactory.getView(page);
		if (v == null) {
			SystemHtmlPage.responseSystemPage(request, response, templateFactory.getConfig().getCharset(),
					HttpServletResponse.SC_NOT_FOUND, "template: " + page + "not found");
			return;
		}
		
		response.setCharacterEncoding(templateFactory.getConfig().getCharset());
		response.setHeader("Content-Type", "text/html; charset=" + templateFactory.getConfig().getCharset());
		ServletOutputStream out = response.getOutputStream();
		Model model = new Model() {

			@Override
			public void clear() {
				Enumeration<String> e = request.getAttributeNames();
				while (e.hasMoreElements()) {
					String name = e.nextElement();
					request.removeAttribute(name);
				}
			}

			@Override
			public Object get(String name) {
				return request.getAttribute(name);
			}

			@Override
			public void put(String name, Object o) {
				request.setAttribute(name, o);
			}

			@Override
			public void remove(String name) {
				request.removeAttribute(name);
			}
		};
		try {
			v.render(model, out);
		} finally {
			out.close();
		}
	}

}
