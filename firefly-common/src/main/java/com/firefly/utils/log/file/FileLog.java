package com.firefly.utils.log.file;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

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
	private Queue<LogItem> buffer = new LinkedList<LogItem>();
	private static final int BATCH_SIZE = 1024;
	private BufferedWriter bufferedWriter;
	private String currentDate = LogFactory.dayDateFormat.format(new Date());
	

	public void write(LogItem logItem) {
		if (fileOutput)
			buffer.offer(logItem);
		if (fileOutput && buffer.size() >= BATCH_SIZE) {
//			System.out.println("the buffer is full");
			flush();
		}
	}

	public void flush() {
		if (fileOutput && buffer.size() > 0) {
			try {
				for (LogItem logItem = null; (logItem = buffer.poll()) != null;) {
					Date d = new Date();
					bufferedWriter = getBufferedWriter(LogFactory.dayDateFormat.format(d));
					logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(d));
					bufferedWriter.append(logItem.toString() + CL);
				}
				bufferedWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
				bufferedWriter = new BufferedWriter(new FileWriter(file, true));
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
		if (consoleOutput) {
			item.setDate(SafeSimpleDateFormat.defaultDateFormat.format(new Date()));
			System.out.println(item.toString());
		}
		if (fileOutput)
			LogFactory.getInstance().getLogTask().add(item);
	}

	@Override
	public void trace(String str) {
		if (level > Log.TRACE)
			return;
		add(str, "TRACE", null, new Object[0]);
	}

	@Override
	public void trace(String str, Object... objs) {
		if (level > Log.TRACE)
			return;
		add(str, "TRACE", null, objs);
	}

	@Override
	public void trace(String str, Throwable throwable, Object... objs) {
		if (level > Log.TRACE)
			return;
		add(str, "TRACE", null, objs);
	}

	@Override
	public void debug(String str) {
		if (level > Log.DEBUG)
			return;
		add(str, "DEBUG", null, new Object[0]);
	}

	@Override
	public void debug(String str, Object... objs) {
		if (level > Log.DEBUG)
			return;
		add(str, "DEBUG", null, objs);
	}

	@Override
	public void debug(String str, Throwable throwable, Object... objs) {
		if (level > Log.DEBUG)
			return;
		add(str, "DEBUG", throwable, objs);
	}

	@Override
	public void info(String str) {
		if (level > Log.INFO)
			return;
		add(str, "INFO", null, new Object[0]);
	}

	@Override
	public void info(String str, Object... objs) {
		if (level > Log.INFO)
			return;
		add(str, "INFO", null, objs);
	}

	@Override
	public void info(String str, Throwable throwable, Object... objs) {
		if (level > Log.INFO)
			return;
		add(str, "INFO", throwable, objs);
	}

	@Override
	public void warn(String str) {
		if (level > Log.WARN)
			return;
		add(str, "WARN", null, new Object[0]);
	}

	@Override
	public void warn(String str, Object... objs) {
		if (level > Log.WARN)
			return;
		add(str, "WARN", null, objs);
	}

	@Override
	public void warn(String str, Throwable throwable, Object... objs) {
		if (level > Log.WARN)
			return;
		add(str, "WARN", throwable, objs);
	}

	@Override
	public void error(String str, Object... objs) {
		if (level > Log.ERROR)
			return;
		add(str, "ERROR", null, objs);
	}

	@Override
	public void error(String str, Throwable throwable, Object... objs) {
		if (level > Log.ERROR)
			return;
		add(str, "ERROR", throwable, objs);
	}

	@Override
	public void error(String str) {
		if (level > Log.ERROR)
			return;
		add(str, "ERROR", null, new Object[0]);
	}

	@Override
	public boolean isTraceEnable() {
		return level > Log.TRACE;
	}

	@Override
	public boolean isDebugEnable() {
		return level > Log.DEBUG;
	}

	@Override
	public boolean isInfoEnable() {
		return level > Log.INFO;
	}

	@Override
	public boolean isWarnEnable() {
		return level > Log.WARN;
	}

	@Override
	public boolean isErrorEnable() {
		return level > Log.ERROR;
	}

}
