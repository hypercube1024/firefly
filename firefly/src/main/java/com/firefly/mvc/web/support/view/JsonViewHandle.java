package com.firefly.mvc.web.support.view;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.ViewHandle;
import com.firefly.utils.json.Json;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class JsonViewHandle implements ViewHandle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private String encoding;

	private JsonViewHandle() {

	}

	private static class Holder {
		private static JsonViewHandle instance = new JsonViewHandle();
	}

	public static JsonViewHandle getInstance() {
		return Holder.instance;
	}

	public JsonViewHandle init(String encoding) {
		this.encoding = encoding;
		return this;
	}

	@Override
	public void render(HttpServletRequest request,
			HttpServletResponse response, Object view) throws ServletException,
			IOException {
		if (view != null) {
			log.debug("view [{}]", View.JSON);
			response.setCharacterEncoding(encoding);
			response.setHeader("Content-Type", "application/json; charset="
					+ encoding);
			PrintWriter writer = response.getWriter();
			try{
				writer.print(Json.toJson(view));
			} finally {
				writer.close();
			}
		}
	}

}
