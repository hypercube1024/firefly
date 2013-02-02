package com.firefly.template.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.firefly.template.Config;
import com.firefly.utils.VerifyUtils;

public class JavaFileBuilder {
	private String name;
	private BufferedWriter writer;
	private boolean writeHead = false;
	private StringBuilder tail = new StringBuilder();
	private StringBuilder preBlank = new StringBuilder("\t\t");
	private int textCount = 0;
	private Config config;
	boolean outBreak;

	public JavaFileBuilder(String path, String name, Config config) {
		this.name = name;
		File file = new File(path, name);
		try {
			if (!file.exists())
				file.createNewFile();

			writer = new BufferedWriter(new FileWriter(file));
			this.config = config;
		} catch (Throwable t) {
			Config.LOG.error("java file builder error", t);
		}
	}

	public String getName() {
		return name;
	}

	public JavaFileBuilder write(String str) {
		try {
			writeHead();
			writer.write(str);
		} catch (Throwable t) {
			Config.LOG.error("write error", t);
		}
		return this;
	}

	public StringBuilder getPreBlank() {
		return preBlank;
	}

	public JavaFileBuilder appendTail(String str) {
		tail.append(str);
		return this;
	}

	public JavaFileBuilder writeStringValue(String str) {
		write(preBlank + "out.write(String.valueOf(" + str + ").getBytes(\"" + config.getCharset() + "\"));\n");
		return this;
	}
	
	public String strEscape(String str) {
		StringBuilder sb = new StringBuilder(str.length() * 2);
		sb.append('"');
		
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
			case '\b':
				sb.append('\\');
				sb.append('b');
				break;
			case '\n':
				sb.append('\\');
				sb.append('n');
				break;
			case '\r':
				sb.append('\\');
				sb.append('r');
				break;
			case '\f':
				sb.append('\\');
				sb.append('f');
				break;
			case '\\':
				sb.append('\\');
				sb.append('\\');
				break;
//			case '/':
//				buf[count++] = '\\';
//				buf[count++] = '/';
//				break;
			case '"':
				sb.append('\\');
				sb.append('"');
				break;
			case '\t':
				sb.append('\\');
				sb.append('t');
				break;

			default:
				sb.append(ch);
				break;
			}
		}
		
		sb.append('"');
		return sb.toString();
	}

	public JavaFileBuilder writeText(String str) {
//		str = Arrays.toString(str.getBytes(config.getCharset()));
//		str = str.substring(1, str.length() - 1);
		write(preBlank + "out.write(_TEXT_" + textCount + ");\n")
		.appendTail("\tprivate final byte[] _TEXT_" + textCount + " = str2Byte(" + strEscape(str) + ", \"" + config.getCharset() +"\");\n");
		textCount++;

		return this;
	}

	public JavaFileBuilder writeObject(String el) {
		write(preBlank + "out.write(objNav.getValue(model ,\"" + el
				+ "\").getBytes(\"" + config.getCharset() + "\"));\n");
		return this;
	}
	
	public JavaFileBuilder writeFunction(String functionName, String[] params) {
		write(preBlank + "FunctionRegistry.get(\"" + functionName + "\").render(model, out");
		for (String param : params) {
			param = param.trim();
			if(param.length() > 0) {
				write(", ");
				if(VerifyUtils.isDouble(param) 
						|| VerifyUtils.isLong(param) 
						|| VerifyUtils.isFloat(param) 
						|| VerifyUtils.isInteger(param) 
						|| "null".equals(param)
						|| (param.charAt(0) == '\"' && param.charAt(param.length() - 1) == '\"' )) {
					write(param);
				} else {
					write("objNav.find(model, \"" + param + "\")");
				}
			}
		}
		write(");\n");
		return this;
	}

	public JavaFileBuilder writeObjNav(String el) {
		write("objNav.getValue(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeBooleanObj(String el) {
		write("objNav.getBoolean(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeIntegerObj(String el) {
		write("objNav.getInteger(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeFloatObj(String el) {
		write("objNav.getFloat(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeDoubleObj(String el) {
		write("objNav.getDouble(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeLongObj(String el) {
		write("objNav.getLong(model ,\"" + el + "\")");
		return this;
	}

	public JavaFileBuilder writeTail() {
		try {
			writer.write(tail.toString());
		} catch (Throwable t) {
			Config.LOG.error("write error", t);
		}
		return this;
	}

	private void writeHead() throws IOException {
		if (!writeHead) {
			writer.write("import java.io.OutputStream;\n");
			writer.write("import com.firefly.template.support.ObjectNavigator;\n");
			writer.write("import com.firefly.template.Model;\n");
			writer.write("import com.firefly.template.view.AbstractView;\n");
			writer.write("import com.firefly.template.TemplateFactory;\n");
			writer.write("import com.firefly.template.FunctionRegistry;\n\n");

			String className = name.substring(0, name.length() - 5);

			writer.write("public class " + className + " extends AbstractView {\n\n");
			writer.write("\tpublic " + className + "(TemplateFactory templateFactory){this.templateFactory = templateFactory;}\n\n");
			writer.write("\t@Override\n");
			writer.write("\tprotected void main(Model model, OutputStream out) throws Throwable {\n");
			writer.write("\t\tObjectNavigator objNav = ObjectNavigator.getInstance();\n");
			writeHead = true;
		}
	}

	public void close() {
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable t) {
			Config.LOG.error("java file builder close error", t);
		}
	}

}
