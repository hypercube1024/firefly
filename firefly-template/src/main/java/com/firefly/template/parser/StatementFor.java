package com.firefly.template.parser;

import java.util.HashSet;
import java.util.Set;

import com.firefly.utils.StringUtils;

public class StatementFor implements Statement {

	private static final Set<String> vari = new HashSet<String>();
	
	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		String[] e = StringUtils.split(content, ':');
		e[0] = e[0].trim();
		e[1] = e[1].trim();

		if(vari.contains(e[0])) {
			javaFileBuilder.write(javaFileBuilder.getPreBlank() + e[0] + "_index = -1;\n");
		} else {
			javaFileBuilder.write(javaFileBuilder.getPreBlank() + "int " + e[0] + "_index = -1;\n");
			vari.add(e[0]);
		}
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
