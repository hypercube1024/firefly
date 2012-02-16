package com.firefly.mvc.web.support.view;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.ViewHandle;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JspViewHandle implements ViewHandle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private String viewPath;

	private JspViewHandle() {

	}

	private static class Holder {
		private static JspViewHandle instance = new JspViewHandle();
	}

	public static JspViewHandle getInstance() {
		return Holder.instance;
	}

	public JspViewHandle init(String viewPath) {
		this.viewPath = viewPath;
		return this;
	}

	@Override
	public void render(HttpServletRequest request,
			HttpServletResponse response, Object view) throws ServletException,
			IOException {
		log.debug("view [{}]", View.JSP);
		if (view instanceof String && view != null) {
			String ret = viewPath + view.toString();
			log.debug("jsp path [{}]",ret);
			request.getRequestDispatcher(ret).forward(request, response);
		}
	}

}
