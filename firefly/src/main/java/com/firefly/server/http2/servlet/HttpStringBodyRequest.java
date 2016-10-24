package com.firefly.server.http2.servlet;

import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

public interface HttpStringBodyRequest {

	public String getStringBody();
	
	public String getStringBody(String charset);
	
	public <T> T getJsonBody(Class<T> clazz);
	
	public JsonObject getJsonObjectBody();
	
	public JsonArray getJsonArrayBody();
	
}
