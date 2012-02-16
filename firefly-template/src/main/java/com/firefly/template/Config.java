package com.firefly.template;

import java.io.File;
import java.net.URL;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class Config {
	public static Log LOG = LogFactory.getInstance().getLog("firefly-system");
	private String viewPath;
	private String compiledPath;
	private String suffix = "html";
	private String charset = "UTF-8";
	private String classPath;
	public static final String COMPILED_FOLDER_NAME = "_compiled_view";

	public Config() {
		URL url = this.getClass().getResource("");
		if ("jar".equals(url.getProtocol())) {
			String f = url.getPath();
			try {
				this.classPath = new File(new URL(f.substring(0,
						f.indexOf("!/com/firefly"))).toURI()).getAbsolutePath();
			} catch (Throwable t) {
				LOG.error("template config init error", t);
			}
		}
	}

	public String getViewPath() {
		return viewPath;
	}

	public void setViewPath(String viewPath) {
		char ch = viewPath.charAt(viewPath.length() - 1);
		this.viewPath = (ch == '/' || ch == '\\' ? viewPath : viewPath + "/")
				.replace('\\', '/');
		compiledPath = this.viewPath + COMPILED_FOLDER_NAME;
	}

	public String getCompiledPath() {
		return compiledPath;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getClassPath() {
		return classPath;
	}

	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

}
