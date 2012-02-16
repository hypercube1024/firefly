package com.firefly.template.parser;

import com.firefly.utils.StringUtils;

public class StatementFor implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		String[] e = StringUtils.split(content, ':');
		e[0] = e[0].trim();
		e[1] = e[1].trim();

		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "int " + e[0] + "_index = -1;\n");
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "for(Object "
				+ e[0] + " : objNav.getCollection(model, \""
				+ e[1].substring(e[1].indexOf("${") + 2, e[1].length() - 1)
				+ "\")){\n");
		javaFileBuilder.getPreBlank().append('\t');
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + e[0] + "_index++;\n");
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "model.put(\""
				+ e[0] + "\", " + e[0] + ");\n");
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "model.put(\""
				+ e[0] + "_index\", " + e[0] + "_index);\n");
	}

}
