package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;

public class HTTPResponse {
	
	protected HttpVersion version;
	protected int status;
	protected String reason;
	protected boolean messageCompleted;
	protected boolean headerCompleted;
	protected HttpFields httpFields;
	protected boolean earlyEOF;

	public HttpVersion getVersion() {
		return version;
	}

	public void setVersion(HttpVersion version) {
		this.version = version;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
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

	public HttpFields getHttpFields() {
		return httpFields;
	}

	public void setHttpFields(HttpFields httpFields) {
		this.httpFields = httpFields;
	}

	public boolean isEarlyEOF() {
		return earlyEOF;
	}

	public void setEarlyEOF(boolean earlyEOF) {
		this.earlyEOF = earlyEOF;
	}

}
