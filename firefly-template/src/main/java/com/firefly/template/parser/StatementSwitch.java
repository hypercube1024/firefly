package com.firefly.template.parser;

public class StatementSwitch implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.outBreak = false;
		String obj = content.substring(content.indexOf("${") + 2,
				content.length() - 1);
		javaFileBuilder.write(javaFileBuilder.getPreBlank()
				+ "switch(objNav.getInteger(model, \"" + obj + "\")) {\n");
	}

}
