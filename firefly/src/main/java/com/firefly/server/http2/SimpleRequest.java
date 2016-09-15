package com.firefly.server.http2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

public class SimpleRequest {

	Request request;
	SimpleResponse response;
	HTTPConnection connection;
	Action1<ByteBuffer> content;
	Action1<SimpleRequest> messageComplete;
	List<ByteBuffer> requestBody = new ArrayList<>();

	List<Cookie> cookies;
	String stringBody;

	public SimpleRequest(Request request, Response response, HTTPOutputStream output) {
		this.request = request;
		response.setStatus(HttpStatus.OK_200);
		response.setHttpVersion(HttpVersion.HTTP_1_1);
		this.response = new SimpleResponse(response, output);
	}

	public Request getRequest() {
		return request;
	}

	public SimpleResponse getResponse() {
		return response;
	}

	public HTTPConnection getConnection() {
		return connection;
	}

	public List<ByteBuffer> getRequestBody() {
		return requestBody;
	}

	public SimpleRequest content(Action1<ByteBuffer> content) {
		this.content = content;
		return this;
	}

	public SimpleRequest messageComplete(Action1<SimpleRequest> messageComplete) {
		this.messageComplete = messageComplete;
		return this;
	}

	public String getStringBody(String charset) {
		if (stringBody == null) {
			stringBody = BufferUtils.toString(requestBody, charset);
			return stringBody;
		} else {
			return stringBody;
		}
	}

	public String getStringBody() {
		return getStringBody("UTF-8");
	}

	public <T> T getJsonBody(Class<T> clazz) {
		return Json.toObject(getStringBody(), clazz);
	}

	public JsonObject getJsonObjectBody() {
		return Json.toJsonObject(getStringBody());
	}

	public JsonArray getJsonArrayBody() {
		return Json.toJsonArray(getStringBody());
	}

	public List<Cookie> getCookies() {
		if (cookies == null) {
			String v = request.getFields().get(HttpHeader.COOKIE);
			cookies = CookieParser.parseCookie(v);
			return cookies;
		} else {
			return cookies;
		}
	}
}
