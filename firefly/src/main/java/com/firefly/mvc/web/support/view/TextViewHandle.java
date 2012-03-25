package com.firefly.mvc.web.support.view;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.ViewHandle;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TextViewHandle implements ViewHandle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private String encoding;

	private TextViewHandle() {

	}

	private static class Holder {
		private static TextViewHandle instance = new TextViewHandle();
	}

	public static TextViewHandle getInstance() {
		return Holder.instance;
	}

	public TextViewHandle init(String encoding) {
		this.encoding = encoding;
		return this;
	}

	@Override
	public void render(HttpServletRequest request,
			HttpServletResponse response, Object view) throws ServletException,
			IOException {
		if (view instanceof String && view != null) {
			log.debug("view [{}]", View.TEXT);
			response.setCharacterEncoding(encoding);
			response.setHeader("Content-Type", "text/plain; charset="
					+ encoding);
			PrintWriter writer = response.getWriter();
			try {
				writer.print(view.toString());
			} finally {
				writer.close();
			}
		}

	}

}
