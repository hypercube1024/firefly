package com.firefly.template.parser;

public interface Statement {
	void parse(String content, JavaFileBuilder javaFileBuilder);
}
