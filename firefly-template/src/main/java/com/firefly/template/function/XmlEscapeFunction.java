package com.firefly.template.function;

import java.io.OutputStream;

import com.firefly.template.Function;
import com.firefly.template.Model;
import com.firefly.utils.StringUtils;

public class XmlEscapeFunction implements Function {

	private String charset;

	public XmlEscapeFunction(String charset) {
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable {
		String str = (String)obj[0];
		out.write(StringUtils.escapeXML(str).getBytes(charset));
	}

}
