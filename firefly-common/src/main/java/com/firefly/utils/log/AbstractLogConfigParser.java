package com.firefly.utils.log;

import java.io.File;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.file.FileLog;

public abstract class AbstractLogConfigParser implements LogConfigParser {

	protected FileLog createLog(String name, String level, String path, boolean console, int maxFileSize) {
		FileLog fileLog = new FileLog();
		fileLog.setName(name);
		fileLog.setLevel(LogLevel.fromName(level));
		fileLog.setMaxFileSize(maxFileSize);

		boolean createLogDirectorySuccess = false;
		if (VerifyUtils.isNotEmpty(path)) {
			File file = new File(path);
			createLogDirectorySuccess = createLogDirectory(file);
			if (createLogDirectorySuccess) {
				fileLog.setPath(path);
				fileLog.setFileOutput(true);
			} else {
				createLogDirectorySuccess = createLogDirectory(DEFAULT_LOG_DIRECTORY);
				if (createLogDirectorySuccess) {
					fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
					fileLog.setFileOutput(true);
				} else {
					fileLog.setFileOutput(false);
				}
			}
		} else {
			createLogDirectorySuccess = createLogDirectory(DEFAULT_LOG_DIRECTORY);
			if (createLogDirectorySuccess) {
				fileLog.setPath(DEFAULT_LOG_DIRECTORY.getAbsolutePath());
				fileLog.setFileOutput(true);
			} else {
				fileLog.setFileOutput(false);
			}
		}

		if (createLogDirectorySuccess) {
			fileLog.setConsoleOutput(console);
		} else {
			fileLog.setConsoleOutput(true);
			System.err.println("create log directory is failure");
		}

		System.out.println("initialize log " + fileLog.toString());
		return fileLog;
	}

	protected boolean createLogDirectory(File file) {
		if (file.exists() && file.isDirectory()) {
			return true;
		} else {
			return file.mkdirs();
		}
	}

	@Override
	public FileLog createDefaultLog() {
		return createLog(DEFAULT_LOG_NAME, DEFAULT_LOG_LEVEL, null, false, DEFAULT_MAX_FILE_SIZE);
	}

}
