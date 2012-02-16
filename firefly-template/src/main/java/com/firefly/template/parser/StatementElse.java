package com.firefly.template.parser;

public class StatementElse implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.write(javaFileBuilder.getPreBlank().deleteCharAt(0).toString() + "} else {\n");
		javaFileBuilder.getPreBlank().append('\t');
	}

}
