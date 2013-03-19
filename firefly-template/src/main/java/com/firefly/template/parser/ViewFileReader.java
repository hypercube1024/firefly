package com.firefly.template.parser;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import com.firefly.template.Config;
import com.firefly.template.exception.TemplateFileReadException;
import com.firefly.template.support.CompileUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.LineReaderHandler;

public class ViewFileReader {
	private Config config;
	private List<String> javaFiles = new ArrayList<String>();
	private List<String> templateFiles = new ArrayList<String>();
	private List<String> classNames = new ArrayList<String>();
	private List<String> javaFiles0 = new ArrayList<String>();

	public ViewFileReader(Config config) {
		this.config = config;
		if (init() != 0)
			throw new TemplateFileReadException("template file parse error");
	}

	private int init() {
		int ret = 0;
		File file = new File(config.getCompiledPath());
		if (!file.exists()) {
			file.mkdir();
		}
		read0(new File(config.getViewPath()));
		if(javaFiles0.size() > 0)
			ret = CompileUtils.compile(config.getCompiledPath(), config.getClassPath(), config.getCharset(), javaFiles0);
		return ret;
	}

	public List<String> getJavaFiles() {
		return javaFiles;
	}

	public List<String> getTemplateFiles() {
		return templateFiles;
	}

	public List<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(List<String> classNames) {
		this.classNames = classNames;
	}

	private void read0(File file) {
		file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					read0(f);
				} else if (f.getName().endsWith("." + config.getSuffix())) {
					parse(f);
				}
				return false;
			}
		});
	}

	private void parse(File f) {
		String name = f.getAbsolutePath().replace('\\', '/');
		templateFiles.add(name.substring(config.getViewPath().length() - 1));
		
		name = name.substring(config.getViewPath().length() - 1,
				name.length() - config.getSuffix().length()).replace('/', '_')
				+ "java";
		classNames.add(name.substring(0, name.length() - 5));
		
		String javaFile = config.getCompiledPath() + "/" + name;
		javaFiles.add(javaFile);
		
		String classFileName = javaFile.substring(0, javaFile.length() - 4) + "class";
		File classFile = new File(classFileName);
		if(classFile.exists() && classFile.lastModified() >= f.lastModified()) {
//			System.out.println(classFile.getAbsolutePath() + "|" +  classFile.lastModified() + "|" + f.lastModified());
			return;
		}
		javaFiles0.add(javaFile);
		// System.out.println("======= " + name + " =======");

		JavaFileBuilder javaFileBuilder = new JavaFileBuilder(
				config.getCompiledPath(), name, config);
		TemplateFileLineReaderHandler lineReaderHandler = new TemplateFileLineReaderHandler(
				javaFileBuilder);

		try {
			FileUtils.read(f, lineReaderHandler, config.getCharset());
		} catch (Throwable t) {
			Config.LOG.error("view file read error", t);
		} finally {
			lineReaderHandler.close();
		}
	}

	private void parseComment(String comment, JavaFileBuilder javaFileBuilder) {
		int start = comment.indexOf('#');
		int end = 0;
		if (start >= 0) {
			for (int i = start; i < comment.length(); i++) {
				if (comment.charAt(i) == ' ' || comment.charAt(i) == '\t'
						|| comment.charAt(i) == '\r'
						|| comment.charAt(i) == '\n') {
					end = i;
					break;
				}
			}
			if (end <= start)
				end = comment.length();

			String keyword = comment.substring(start, end);
			String content = comment.substring(end).trim();
			// System.out.println(comment.length() + "|1|comment:\t" + keyword
			// + " " + content);
			StateMachine.parse(keyword, content, javaFileBuilder);
		}
	}

	private void parseText(String text, JavaFileBuilder javaFileBuilder) {
		int cursor = 0;
		String t = null;
		for (int start, end; (start = text.indexOf("${", cursor)) != -1
				&& (end = text.indexOf("}", start)) != -1;) {
			t = text.substring(cursor, start);
			if (t.length() > 0) {
				javaFileBuilder.writeText(t);
			}
			String e = text.substring(start + 2, end);
			int l = e.indexOf('(');
			if(l > 0) {
				int r = e.indexOf(')');
				if(r > l) { // function
					String functionName = e.substring(0, l);
					String[] params = StringUtils.split(e.substring(l + 1, r), ',');
					javaFileBuilder.writeFunction(functionName, params);
				}
			} else {
				javaFileBuilder.writeObject(e);
			}
			cursor = end + 1;
		}
		if(cursor < text.length()) {
			t = text.substring(cursor, text.length());
			if (t.length() > 0)
				javaFileBuilder.writeText(t);
		}
	}

	private enum ParserStatus {
		INIT, COMMENT_PARSING
	}
	
	private class TemplateFileLineReaderHandler implements LineReaderHandler, Closeable {
		private JavaFileBuilder javaFileBuilder;
		private StringBuilder text = new StringBuilder();
		private StringBuilder comment = new StringBuilder();
		private ParserStatus status = ParserStatus.INIT;
		

		public TemplateFileLineReaderHandler(JavaFileBuilder javaFileBuilder) {
			this.javaFileBuilder = javaFileBuilder;
		}

		@Override
		public void close() {
			if (text.length() > 0) {
				parseText(text.toString(), javaFileBuilder);
			}

			javaFileBuilder.write("\t}\n\n").writeTail().write("}");
			javaFileBuilder.close();
		}

		@Override
		public void readline(String line, int num) {
			switch (status) {
			case INIT:
				int i = line.indexOf("<!--");

				if (i >= 0) { // html comment parse start
					text.append(line.substring(0, i).trim()); // at here assert is not '\n'
					if (text.length() > 0) {
						parseText(text.toString(), javaFileBuilder);
						text = new StringBuilder();
					}

					int j = line.indexOf("-->");
					if (j > i + 4) { // html comment end
						assert comment.length() == 0;
						parseComment(line.substring(i + 4, j).trim(), javaFileBuilder);
						// continue parse template after the end of comment
						if(j + 3 < line.length()) {
							readline(line.substring(j + 3).trim() + "\n", num);
						}
					} else {
						status = ParserStatus.COMMENT_PARSING;
						comment.append(line.substring(i + 4).trim() + "\n");
					}
				} else {
					text.append(line.trim() + "\n"); // save the character '\n' in template
				}
				break;
			case COMMENT_PARSING: // comment parsing
				int j = line.indexOf("-->");
				if (j >= 0) { // html comment end
					status = ParserStatus.INIT;
					comment.append(line.substring(0, j).trim());
					parseComment(comment.toString(), javaFileBuilder);
					comment = new StringBuilder();
					// continue parse template after the end of comment
					if(j + 3 < line.length()) {
						readline(line.substring(j + 3).trim() + "\n", num);
					}
				} else
					comment.append(line.trim() + "\n");
				break;
			default:
				break;
			}

		}
	}
}
