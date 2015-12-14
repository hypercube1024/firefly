package com.firefly.utils.log.file;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.log.LogItem;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class FileLog implements Log, Closeable {
	private int level;
	private String path;
	private String name;
	private boolean consoleOutput;
	private boolean fileOutput;
	private BufferedWriter bufferedWriter;
	private String currentDate = LogFactory.dayDateFormat.format(new Date());
	private static final int bufferSize = 512 * 1024;
	

	void write(LogItem logItem) {
		if (consoleOutput) {
//			System.out.println("test...");
			logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(new Date()));
			System.out.println(logItem.toString());
		}
		
		if (!fileOutput)
			return;
		
		Date d = new Date();
		try {
			bufferedWriter = getBufferedWriter(LogFactory.dayDateFormat.format(d));
			logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(d));
			bufferedWriter.append(logItem.toString() + CL);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void flush() throws IOException {
		if(bufferedWriter != null)
			bufferedWriter.flush();
	}
	
	@Override
	public void close() throws IOException {
		if(bufferedWriter != null)
			bufferedWriter.close();
	}

	private BufferedWriter getBufferedWriter(String newDate) throws IOException {
		if(bufferedWriter == null || !currentDate.equals(newDate)) {
			File file = new File(path, name + "." + newDate + ".txt");
			boolean ret = true;
			if (!file.exists()) {
				ret = file.createNewFile();
			}
			if (ret) {
				close();
				bufferedWriter = new BufferedWriter(new FileWriter(file, true), bufferSize);
				currentDate = newDate;
				System.out.println("get new buffered " + file.getName());
			}
		}
		return bufferedWriter;
	}
	
	public void setConsoleOutput(boolean consoleOutput) {
		this.consoleOutput = consoleOutput;
	}

	public void setFileOutput(boolean fileOutput) {
		this.fileOutput = fileOutput;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private void add(String str, String level, Throwable throwable,
			Object... objs) {
		LogItem item = new LogItem();
		item.setLevel(level);
		item.setName(name);
		item.setContent(str);
		item.setObjs(objs);
		item.setThrowable(throwable);
		LogFactory.getInstance().getLogTask().add(item);
	}

	@Override
	public void trace(String str) {
		if (!isTraceEnable())
			return;
		add(str, "TRACE", null, new Object[0]);
	}

	@Override
	public void trace(String str, Object... objs) {
		if (!isTraceEnable())
			return;
		add(str, "TRACE", null, objs);
	}

	@Override
	public void trace(String str, Throwable throwable, Object... objs) {
		if (!isTraceEnable())
			return;
		add(str, "TRACE", null, objs);
	}

	@Override
	public void debug(String str) {
		if (!isDebugEnable())
			return;
		add(str, "DEBUG", null, new Object[0]);
	}

	@Override
	public void debug(String str, Object... objs) {
		if (!isDebugEnable())
			return;
		add(str, "DEBUG", null, objs);
	}

	@Override
	public void debug(String str, Throwable throwable, Object... objs) {
		if (!isDebugEnable())
			return;
		add(str, "DEBUG", throwable, objs);
	}

	@Override
	public void info(String str) {
		if (!isInfoEnable())
			return;
		add(str, "INFO", null, new Object[0]);
	}

	@Override
	public void info(String str, Object... objs) {
		if (!isInfoEnable())
			return;
		add(str, "INFO", null, objs);
	}

	@Override
	public void info(String str, Throwable throwable, Object... objs) {
		if (!isInfoEnable())
			return;
		add(str, "INFO", throwable, objs);
	}

	@Override
	public void warn(String str) {
		if (!isWarnEnable())
			return;
		add(str, "WARN", null, new Object[0]);
	}

	@Override
	public void warn(String str, Object... objs) {
		if (!isWarnEnable())
			return;
		add(str, "WARN", null, objs);
	}

	@Override
	public void warn(String str, Throwable throwable, Object... objs) {
		if (!isWarnEnable())
			return;
		add(str, "WARN", throwable, objs);
	}

	@Override
	public void error(String str, Object... objs) {
		if (!isErrorEnable())
			return;
		add(str, "ERROR", null, objs);
	}

	@Override
	public void error(String str, Throwable throwable, Object... objs) {
		if (!isErrorEnable())
			return;
		add(str, "ERROR", throwable, objs);
	}

	@Override
	public void error(String str) {
		if (!isErrorEnable())
			return;
		add(str, "ERROR", null, new Object[0]);
	}

	@Override
	public boolean isTraceEnable() {
		return level <= Log.TRACE;
	}

	@Override
	public boolean isDebugEnable() {
		return level <= Log.DEBUG;
	}

	@Override
	public boolean isInfoEnable() {
		return level <= Log.INFO;
	}

	@Override
	public boolean isWarnEnable() {
		return level <= Log.WARN;
	}

	@Override
	public boolean isErrorEnable() {
		return level <= Log.ERROR;
	}

}
