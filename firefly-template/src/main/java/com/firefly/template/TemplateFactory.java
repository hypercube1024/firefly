package com.firefly.template;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firefly.template.function.DateFormatFunction;
import com.firefly.template.function.LengthFunction;
import com.firefly.template.parser.ViewFileReader;

public class TemplateFactory {

	private Config config;
	private Map<String, View> map = new HashMap<String, View>();

	public TemplateFactory() {

	}
	
	public TemplateFactory(File file) {
		this.config = new Config();
		config.setViewPath(file.getAbsolutePath());
	}

	public TemplateFactory(String path) {
		this.config = new Config();
		config.setViewPath(path);
	}

	public TemplateFactory(Config config) {
		this.config = config;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}
	
	public TemplateFactory init() {
		if(config == null)
			throw new IllegalArgumentException("template config is null");
		
		long start = System.currentTimeMillis();
		FunctionRegistry.add("dateFormat", new DateFormatFunction(config.getCharset()));
		FunctionRegistry.add("len", new LengthFunction(config.getCharset()));
		ViewFileReader reader = new ViewFileReader(config);
		List<String> javaFiles = reader.getJavaFiles();
		List<String> templateFiles = reader.getTemplateFiles();
		List<String> classNames = reader.getClassNames();
		
		for (int i = 0; i < javaFiles.size(); i++) {
			String c = javaFiles.get(i);
			final String classFileName = c.substring(0, c.length() - 4) + "class";
			ClassLoader classLoader = new TemplateClassLoader(classFileName, TemplateFactory.class.getClassLoader());
			
			try {
				map.put(templateFiles.get(i), (View)classLoader.loadClass(classNames.get(i)).getConstructor(TemplateFactory.class).newInstance(this));
			} catch (Throwable e) {
				Config.LOG.error("load class error", e);
			}
		}
		long end = System.currentTimeMillis();
		Config.LOG.info("firefly-template init in {} ms", (end - start));
		return this;
	}
	
	public View getView(String name) {
		return map.get(name);
	}
	
	private class TemplateClassLoader extends ClassLoader {
		private String classFileName;
		
		public TemplateClassLoader(String classFileName, ClassLoader classLoader) {
			super(classLoader);
			this.classFileName = classFileName;
		}
		
		@Override
		public Class<?> findClass(String name) {
			BufferedInputStream bis = null;
			byte[] b = null;
			try {
				File file = new File(classFileName);
				b = new byte[(int)file.length()];
				bis = new BufferedInputStream(new FileInputStream(file));
				bis.read(b);
			} catch (Throwable e) {
				Config.LOG.error("read class file error", e);
			} finally {
				if(bis != null)
					try {
						bis.close();
					} catch (IOException e) {
						Config.LOG.error("close error", e);
					}
			}
			
			return defineClass(name, b, 0, b.length);
		}
	}

}
