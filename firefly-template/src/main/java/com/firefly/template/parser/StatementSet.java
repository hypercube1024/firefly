package com.firefly.template.parser;

import com.firefly.utils.StringUtils;

public class StatementSet implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		String[] params = StringUtils.split(content, '&');
		for (String param : params) {
			String[] v = StringUtils.split(param, '=');
			if (v[1].length() > 3 && v[1].charAt(0) == '$'
					&& v[1].charAt(1) == '{'
					&& v[1].charAt(v[1].length() - 1) == '}') {
				javaFileBuilder.write(javaFileBuilder.getPreBlank()
						+ "model.put(\"" + v[0] + "\", objNav.find(model, \""
						+ v[1].substring(2, v[1].length() - 1) + "\"));\n");
			} else {
				javaFileBuilder.write(javaFileBuilder.getPreBlank()
						+ "model.put(\"" + v[0] + "\", \"" + v[1] + "\");\n");
			}
		}

	}

}
