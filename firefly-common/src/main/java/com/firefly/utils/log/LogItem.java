package com.firefly.utils.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.firefly.utils.StringUtils;

public class LogItem {
	private String name, content, date, level;
	private Object[] objs;
	private Throwable throwable;
	private String logStr;

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public void setObjs(Object[] objs) {
		this.objs = objs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public String toString() {
		if (logStr == null) {
			content = StringUtils.replace(content, objs);
			if (throwable != null) {
				StringWriter str = new StringWriter();
				PrintWriter out = new PrintWriter(str);
				try {
					out.println();
					out.println("$err_start");
					throwable.printStackTrace(out);
					out.println("$err_end");
				} finally {
					out.close();
				}
				content += str.toString();
			}

			logStr = level + " " + date + "\t" + content;
		}
		return logStr;
	}

}
