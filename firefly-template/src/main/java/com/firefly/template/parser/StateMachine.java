package com.firefly.template.parser;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {
	private static final Map<String, Statement> MAP = new HashMap<String, Statement>();

	static {
		MAP.put("#eval", new StatementExpression());
		MAP.put("#if", new StatementIf());
		MAP.put("#elseif", new StatementElseIf());
		MAP.put("#else", new StatementElse());
		MAP.put("#for", new StatementFor());
		MAP.put("#switch", new StatementSwitch());
		MAP.put("#case", new StatementSwitchCase());
		MAP.put("#default", new StatementSwitchDefault());
		MAP.put("#end", new StatementEnd());
		MAP.put("#set", new StatementSet());
		MAP.put("#include", new StatementInclude());
	}

	public static void parse(String keyword, String content,
			JavaFileBuilder javaFileBuilder) {
		Statement statement = MAP.get(keyword);
		if (statement != null)
			statement.parse(content, javaFileBuilder);
	}
}
