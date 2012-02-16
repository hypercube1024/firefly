package com.firefly.template.parser;

public class StatementElseIf extends StatementIf {
	
	@Override
	protected void writePrefix(JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.write(javaFileBuilder.getPreBlank().deleteCharAt(0).toString() + "} else if (");
	}
}
