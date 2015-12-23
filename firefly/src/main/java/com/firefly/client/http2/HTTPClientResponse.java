package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPClientResponse extends MetaData.Response {
	
	protected boolean messageCompleted;
	protected boolean headerCompleted;
	protected boolean earlyEOF;
	
	public HTTPClientResponse(HttpVersion version, int status, String reason) {
		super(version, status, reason, new HttpFields(), -1);
	}
	
	public HTTPClientResponse(HttpVersion version, int status, String reason, HttpFields fields, long contentLength) {
		super(version, status, reason, fields, contentLength);
	}

	public boolean isMessageCompleted() {
		return messageCompleted;
	}

	public void setMessageCompleted(boolean messageCompleted) {
		this.messageCompleted = messageCompleted;
	}

	public boolean isHeaderCompleted() {
		return headerCompleted;
	}

	public void setHeaderCompleted(boolean headerCompleted) {
		this.headerCompleted = headerCompleted;
	}

	public boolean isEarlyEOF() {
		return earlyEOF;
	}

	public void setEarlyEOF(boolean earlyEOF) {
		this.earlyEOF = earlyEOF;
	}

}
