package com.firefly.template.parser;

public class StatementEnd implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.write(javaFileBuilder.getPreBlank().deleteCharAt(0).toString() + "}\n");
	}

}
