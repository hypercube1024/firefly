package com.firefly.mvc.web.view;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TemplateView implements View {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static TemplateFactory t;
	private static boolean init = false;
	
	private String page;
	
	public static void init(String viewPath, String encoding) {
		if (!init) {
			log.info("template path {}", viewPath);
			com.firefly.template.Config config = new com.firefly.template.Config();
			config.setViewPath(viewPath);
			config.setCharset(encoding);
			t = new TemplateFactory(config).init();
			init = true;
		}
	}
	
	public TemplateView(String page) {
		this.page = page;
	}

	@Override
	public void render(final HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		com.firefly.template.View v = t.getView(page);
		if (v == null) {
			SystemHtmlPage.responseSystemPage(request, response, t.getConfig().getCharset(),
					HttpServletResponse.SC_NOT_FOUND, "template: " + page + "not found");
			return;
		}
		
		response.setCharacterEncoding(t.getConfig().getCharset());
		response.setHeader("Content-Type", "text/html; charset=" + t.getConfig().getCharset());
		ServletOutputStream out = response.getOutputStream();
		Model model = new Model() {

			@SuppressWarnings("unchecked")
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
