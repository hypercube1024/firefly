package test.utils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import static org.hamcrest.Matchers.*;
import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.CompilerUtils.JavaSourceFromString;

public class TestCompilerUtils {
	
	@Test
	public void test() throws Throwable {
		String source = "package com.test;\n" 
				+ "public class Say {"
						+ "public String hello() {" 
						+ "System.out.println(\"hello\");"
						+ "return \"world\";" 
						+ "}" 
				+ "}";

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		JavaFileManager fileManager = CompilerUtils.getStringSourceJavaFileManager(compiler, null, null, Charset.forName("UTF-8"));
		boolean result = false;
		try {
			CompilationTask task = compiler.getTask(null, fileManager, null, null, null,Arrays.asList(new JavaSourceFromString("com.test.Say", source)));
			result = task.call();
		} finally {
			fileManager.close();
		}

		if (!result)
			return;

		
		Class<?> clazz = CompilerUtils.getClassByName("com.test.Say");
		Object obj = clazz.newInstance();
		Method method = clazz.getMethod("hello");
		String str = (String)method.invoke(obj);
		Assert.assertThat(str, is("world"));
	}
}
