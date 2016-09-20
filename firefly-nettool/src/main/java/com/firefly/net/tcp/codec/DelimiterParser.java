package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;

public class DelimiterParser implements MessageHandler<String, String> {

	protected StringBuilder buffer = new StringBuilder();
	protected String delimiter;
	protected Action1<String> complete;

	public DelimiterParser(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public void receive(String s) {
		buffer.append(s);
		int cursor = 0;
		int start = 0;
		while ((start = buffer.indexOf(delimiter, cursor)) != -1) {
			complete.call(buffer.substring(cursor, start));
			cursor = start + delimiter.length();
		}
		if (cursor < s.length()) {
			String remain = s.substring(cursor, s.length());
			buffer.delete(0, buffer.length());
			buffer.append(remain);
		} else {
			buffer.delete(0, buffer.length());
		}
	}

	@Override
	public void complete(Action1<String> complete) {
		this.complete = complete;
	}

}
