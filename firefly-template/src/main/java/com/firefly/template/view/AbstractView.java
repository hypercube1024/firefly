package com.firefly.template.view;

import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.template.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public abstract class AbstractView implements View {

	private static Logger log = LoggerFactory.getLogger("firefly-system");
	
	protected TemplateFactory templateFactory;

	@Override
	public void render(Model model, OutputStream out) {
		try {
			main(model, out);
		} catch (Throwable t) {
			log.error("view render error", t);
		}
	}
	
	public static byte[] str2Byte(String str, String charset) {
		byte[] ret = null;
		try {
			ret = str.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			log.error("write text error", e);
		}
		return ret;
	}

	abstract protected void main(Model model, OutputStream out) throws Throwable;

}
