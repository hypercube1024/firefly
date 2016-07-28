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
import com.firefly.utils.log.LogLevel;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class FileLog implements Log, Closeable {

	private LogLevel level;
	private String path;
	private String name;
	private boolean consoleOutput;
	private boolean fileOutput;
	private int maxFileSize;
	private BufferedWriter bufferedWriter;
	private String currentDate = LogFactory.DAY_DATE_FORMAT.format(new Date());
	private static final int bufferSize = 4 * 1024;
	private static final boolean stackTrace = Boolean.getBoolean("debugMode");

	void write(LogItem logItem) {
		if (consoleOutput) {
			logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(new Date()));
			System.out.println(logItem.toString());
		}

		if (fileOutput) {
			Date d = new Date();
			boolean success = getBufferedWriter(LogFactory.DAY_DATE_FORMAT.format(d));
			if (success) {
				logItem.setDate(SafeSimpleDateFormat.defaultDateFormat.format(d));
				try {
					bufferedWriter.append(logItem.toString() + CL);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("The log " + toString() + " can not get buffered writer!");
			}
		}
	}

	public void flush() {
		if (bufferedWriter != null) {
			try {
				bufferedWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() {
		if (bufferedWriter != null) {
			try {
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean createNewBufferedWriter(String newDate) {
		File file = new File(path, name + "." + newDate + ".txt");
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file, true), bufferSize);
			currentDate = newDate;
			System.out.println("get new log buffer, the file path is " + file.getAbsolutePath());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean getBufferedWriter(String newDate) {
		if (bufferedWriter == null) {
			return createNewBufferedWriter(newDate);
		} else {
			if (!currentDate.equals(newDate)) {
				close();
				return createNewBufferedWriter(newDate);
			} else {
				return true;
			}
		}
	}

	public void setConsoleOutput(boolean consoleOutput) {
		this.consoleOutput = consoleOutput;
	}

	public void setFileOutput(boolean fileOutput) {
		this.fileOutput = fileOutput;
	}

	public LogLevel getLevel() {
		return level;
	}

	public void setLevel(LogLevel level) {
		this.level = level;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(int maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	private void add(String str, String level, Throwable throwable, Object... objs) {
		LogItem item = new LogItem();
		item.setLevel(level);
		item.setName(name);
		item.setContent(str);
		item.setObjs(objs);
		item.setThrowable(throwable);
		if (stackTrace) {
			item.setStackTraceElement(getStackTraceElement());
		}
		LogFactory.getInstance().getLogTask().add(item);
	}

	@Override
	public void trace(String str) {
		if (!isTraceEnabled())
			return;
		add(str, LogLevel.TRACE.getName(), null, new Object[0]);
	}

	@Override
	public void trace(String str, Object... objs) {
		if (!isTraceEnabled())
			return;
		add(str, LogLevel.TRACE.getName(), null, objs);
	}

	@Override
	public void trace(String str, Throwable throwable, Object... objs) {
		if (!isTraceEnabled())
			return;
		add(str, LogLevel.TRACE.getName(), null, objs);
	}

	@Override
	public void debug(String str) {
		if (!isDebugEnabled())
			return;
		add(str, LogLevel.DEBUG.getName(), null, new Object[0]);
	}

	@Override
	public void debug(String str, Object... objs) {
		if (!isDebugEnabled())
			return;
		add(str, LogLevel.DEBUG.getName(), null, objs);
	}

	@Override
	public void debug(String str, Throwable throwable, Object... objs) {
		if (!isDebugEnabled())
			return;
		add(str, LogLevel.DEBUG.getName(), throwable, objs);
	}

	@Override
	public void info(String str) {
		if (!isInfoEnabled())
			return;
		add(str, LogLevel.INFO.getName(), null, new Object[0]);
	}

	@Override
	public void info(String str, Object... objs) {
		if (!isInfoEnabled())
			return;
		add(str, LogLevel.INFO.getName(), null, objs);
	}

	@Override
	public void info(String str, Throwable throwable, Object... objs) {
		if (!isInfoEnabled())
			return;
		add(str, LogLevel.INFO.getName(), throwable, objs);
	}

	@Override
	public void warn(String str) {
		if (!isWarnEnabled())
			return;
		add(str, LogLevel.WARN.getName(), null, new Object[0]);
	}

	@Override
	public void warn(String str, Object... objs) {
		if (!isWarnEnabled())
			return;
		add(str, LogLevel.WARN.getName(), null, objs);
	}

	@Override
	public void warn(String str, Throwable throwable, Object... objs) {
		if (!isWarnEnabled())
			return;
		add(str, LogLevel.WARN.getName(), throwable, objs);
	}

	@Override
	public void error(String str, Object... objs) {
		if (!isErrorEnabled())
			return;
		add(str, LogLevel.ERROR.getName(), null, objs);
	}

	@Override
	public void error(String str, Throwable throwable, Object... objs) {
		if (!isErrorEnabled())
			return;
		add(str, LogLevel.ERROR.getName(), throwable, objs);
	}

	@Override
	public void error(String str) {
		if (!isErrorEnabled())
			return;
		add(str, LogLevel.ERROR.getName(), null, new Object[0]);
	}

	@Override
	public boolean isTraceEnabled() {
		return level.isEnabled(LogLevel.TRACE);
	}

	@Override
	public boolean isDebugEnabled() {
		return level.isEnabled(LogLevel.DEBUG);
	}

	@Override
	public boolean isInfoEnabled() {
		return level.isEnabled(LogLevel.INFO);
	}

	@Override
	public boolean isWarnEnabled() {
		return level.isEnabled(LogLevel.WARN);
	}

	@Override
	public boolean isErrorEnabled() {
		return level.isEnabled(LogLevel.ERROR);
	}

	@Override
	public String toString() {
		return "[level=" + level.getName() + ", name=" + name + ", path=" + path + ", consoleOutput=" + consoleOutput
				+ ", fileOutput=" + fileOutput + ", maxFileSize=" + maxFileSize + "]";
	}

	public static StackTraceElement getStackTraceElement() {
		return Thread.currentThread().getStackTrace()[4];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (consoleOutput ? 1231 : 1237);
		result = prime * result + (fileOutput ? 1231 : 1237);
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + maxFileSize;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileLog other = (FileLog) obj;
		if (consoleOutput != other.consoleOutput)
			return false;
		if (fileOutput != other.fileOutput)
			return false;
		if (level != other.level)
			return false;
		if (maxFileSize != other.maxFileSize)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}
