package com.firefly.template.parser;

import com.firefly.utils.StringUtils;

public class StatementInclude implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		String[] p = StringUtils.split(content, '?');
		if (p.length > 1)
			StateMachine.parse("#set", p[1], javaFileBuilder);

		javaFileBuilder.write(javaFileBuilder.getPreBlank() + "templateFactory.getView(\"" + p[0] + "\").render(model, out);\n");
	}

}
