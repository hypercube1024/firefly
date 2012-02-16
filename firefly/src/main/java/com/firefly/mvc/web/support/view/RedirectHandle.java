package com.firefly.mvc.web.support.view;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.ViewHandle;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class RedirectHandle implements ViewHandle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private RedirectHandle() {

	}

	private static class Holder {
		private static RedirectHandle instance = new RedirectHandle();
	}

	public static RedirectHandle getInstance() {
		return Holder.instance;
	}

	@Override
	public void render(HttpServletRequest request,
			HttpServletResponse response, Object view) throws ServletException,
			IOException {
		log.debug("view [{}]", View.REDIRECT);
		if (view instanceof String && view != null) {
			response.sendRedirect(request.getContextPath()
					+ request.getServletPath() + view.toString());
		}
	}

}
