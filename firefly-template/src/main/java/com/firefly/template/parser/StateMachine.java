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

	/**
	 * 
	 * @param keyword 
	 * 			keyword of the template language 
	 * @param content
	 * 			expression of the template language  
	 * @param javaFileBuilder
	 * @return true if current word is keyword of the template language, else false.
	 */
	public static boolean parse(String keyword, String content, JavaFileBuilder javaFileBuilder) {
		Statement statement = MAP.get(keyword);
		if(statement == null)
			return false;
		
		statement.parse(content, javaFileBuilder);
		return true;
	}
}
