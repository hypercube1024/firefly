package com.firefly.client.http2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

public class SimpleResponse {

	Response response;
	List<ByteBuffer> responseBody = new ArrayList<>();
	List<Cookie> cookies;
	String stringBody;

	public SimpleResponse(Response response) {
		this.response = response;
	}

	public Response getResponse() {
		return response;
	}

	public List<ByteBuffer> getResponseBody() {
		return responseBody;
	}

	public String getStringBody() {
		return getStringBody("UTF-8");
	}

	public String getStringBody(String charset) {
		if (stringBody == null) {
			stringBody = BufferUtils.toString(responseBody, charset);
			return stringBody;
		} else {
			return stringBody;
		}
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
			cookies = response.getFields().getValuesList(HttpHeader.SET_COOKIE.asString()).stream()
					.map(CookieParser::parseSetCookie).collect(Collectors.toList());
			return cookies;
		} else {
			return cookies;
		}
	}
}
