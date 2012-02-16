package com.firefly.template.parser;

public class StatementIf extends StatementExpression {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		writePrefix(javaFileBuilder);
		content = content.trim();
		javaFileBuilder.write(parse(content));
		javaFileBuilder.write("){\n");
		javaFileBuilder.getPreBlank().append('\t');
	}

	protected void writePrefix(JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "if (");
	}

}
