package com.firefly.template.parser;

public class StatementSwitchCase implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		if (javaFileBuilder.outBreak) {
			javaFileBuilder.write(javaFileBuilder.getPreBlank() + "break;\n");
			javaFileBuilder.getPreBlank().deleteCharAt(0);
		}
		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "case " + content
				+ ":\n");
		javaFileBuilder.outBreak = true;
		javaFileBuilder.getPreBlank().append('\t');
	}

}
