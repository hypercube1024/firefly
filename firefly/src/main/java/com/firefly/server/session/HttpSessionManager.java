package com.firefly.server.session;

import javax.servlet.http.HttpSession;

import com.firefly.server.Config;

public interface HttpSessionManager {
	boolean containsKey(String id);

	HttpSession remove(String id);

	HttpSession get(String id);

	HttpSession create();

	int size();

	Config getConfig();
}
