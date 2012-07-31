package com.firefly.mvc.web.servlet;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.utils.StringUtils;
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
		try {
			ret.append("<!DOCTYPE html><html><head><title>firefly</title></head><body><h2>HTTP ERROR ")
			.append(status)
			.append("</h2><div>")
			.append(StringUtils.escapeXML(URLDecoder.decode(content, "UTF-8")))
			.append("</div><hr/><i><small>firefly framework</small></i></body></html>");
		} catch (UnsupportedEncodingException e) {
			log.error("url decode error", e);
		}
		return ret.toString();
	}
}
