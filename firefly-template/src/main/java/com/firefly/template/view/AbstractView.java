package com.firefly.template.view;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.firefly.template.Config;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.template.View;

public abstract class AbstractView implements View {
	
	protected TemplateFactory templateFactory;

	@Override
	public void render(Model model, OutputStream out) {
		try {
			main(model, out);
		} catch (Throwable t) {
			Config.LOG.error("view render error", t);
		}
	}
	
	public static byte[] str2Byte(String str, String charset) {
		byte[] ret = null;
		try {
			ret = str.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			Config.LOG.error("write text error", e);
		}
		return ret;
	}

	abstract protected void main(Model model, OutputStream out) throws Throwable;

}
