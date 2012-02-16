package com.firefly.mvc.web.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SystemHtmlPage {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void responseSystemPage(HttpServletRequest request,
			HttpServletResponse response, String charset, int status,
			String content) {
		response.setStatus(status);
		response.setCharacterEncoding(charset);
		response.setHeader("Content-Type", "text/html; charset=" + charset);
		PrintWriter writer = null;
		try {
			try {
				writer = response.getWriter();
			} catch (Throwable t) {
				log.error("responseSystemPage error", t);
			}
			writer.print(systemPageTemplate(status, content));
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	public static String systemPageTemplate(int status, String content) {
		StringBuilder ret = new StringBuilder();
		ret.append(
				"<!DOCTYPE html><html><head><title>firefly</title></head><body><h2>HTTP ERROR ")
				.append(status)
				.append("</h2><div>")
				.append(content)
				.append("</div><hr/><i><small>firefly framework</small></i></body></html>");
		return ret.toString();
	}
}
