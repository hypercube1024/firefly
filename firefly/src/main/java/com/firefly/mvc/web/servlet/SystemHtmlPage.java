package com.firefly.mvc.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.view.TemplateView;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SystemHtmlPage {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static Map<Integer, String> errorPage = new HashMap<Integer, String>();
	
	public static void addErrorPage(Integer errorCode, String page) {
		errorPage.put(errorCode, page);
	}
	
	public static void addErrorPageMap(Map<Integer, String> map) {
		errorPage = map;
	}

	public static void responseSystemPage(HttpServletRequest request,
			HttpServletResponse response, String charset, int status,
			String content) {
		response.setStatus(status);
		response.setCharacterEncoding(charset);
		response.setHeader("Content-Type", "text/html; charset=" + charset);
		try (PrintWriter writer = response.getWriter()) {
			writer.print(systemPageTemplate(status, content));
		} catch (IOException e) {
			log.error("response system page exception", e);
		} 
	}

	public static String systemPageTemplate(int status, String content) {
		if(errorPage == null)
			return getDefaultErrorPage(status, content);
		
		String page = errorPage.get(status);
		if(VerifyUtils.isEmpty(page))
			return getDefaultErrorPage(status, content);
		
		TemplateFactory templateFactory = TemplateView.getTemplateFactory();
		if(templateFactory == null)
			return getDefaultErrorPage(status, content);
		
		com.firefly.template.View v = templateFactory.getView(page);
		if(v == null)
			return getDefaultErrorPage(status, content);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		Model model = new Model() {
			private Map<String, Object> map = new HashMap<String, Object>();

			@Override
			public void put(String key, Object object) {
				map.put(key, object);
			}

			@Override
			public Object get(String key) {
				return map.get(key);
			}

			@Override
			public void remove(String key) {
				map.remove(key);
			}

			@Override
			public void clear() {
				map.clear();
			}
		};
		
		try {
			model.put("#systemErrorMessage", URLDecoder.decode(content, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			log.error("url decode error", e);
		}
		
		try {
			v.render(model, out);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				return getDefaultErrorPage(status, content);
			}
		}
		
		try {
			return new String(out.toByteArray(), TemplateView.getCharset());
		} catch (UnsupportedEncodingException e) {
			return getDefaultErrorPage(status, content);
		}
	}
	
	private static String getDefaultErrorPage(int status, String content) {
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
