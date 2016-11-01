package com.firefly.utils.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map.Entry;

import com.firefly.utils.StringUtils;
import com.firefly.utils.function.Action1;
import com.firefly.utils.log.file.FileLog;

public class PropertiesLogConfigParser extends AbstractLogConfigParser {

	@Override
	public boolean parse(Action1<FileLog> action) {
		try {
			Properties properties = loadLogConfigurationFile();
			if (properties != null) {
				parseProperties(properties, action);
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			System.err.println("parse exception, " + e.getMessage());
			return false;
		}
	}

	private Properties loadLogConfigurationFile() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = LogFactory.class.getClassLoader()
				.getResourceAsStream(DEFAULT_PROPERTIES_CONFIG_FILE_NAME)) {
			if (input == null)
				return null;
			properties.load(input);
		}
		return properties;
	}

	private void parseProperties(Properties properties, Action1<FileLog> fileLog) {
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();

			String[] strs = StringUtils.split(value, ',');

			switch (strs.length) {
			case 1:
				fileLog.call(createLog(name, strs[0], null, false, DEFAULT_MAX_FILE_SIZE));
				break;
			case 2:
				if ("console".equalsIgnoreCase(strs[1])) {
					fileLog.call(createLog(name, strs[0], null, true, DEFAULT_MAX_FILE_SIZE));
				} else {
					fileLog.call(createLog(name, strs[0], strs[1], false, DEFAULT_MAX_FILE_SIZE));
				}
				break;
			case 3:
				fileLog.call(
						createLog(name, strs[0], strs[1], "console".equalsIgnoreCase(strs[2]), DEFAULT_MAX_FILE_SIZE));
				break;
			default:
				System.err.println(
						"The log " + name + " configuration format is illegal. It will use default log configuration");
				fileLog.call(createLog(name, DEFAULT_LOG_LEVEL, null, false, DEFAULT_MAX_FILE_SIZE));
				break;
			}
		}
	}

}
