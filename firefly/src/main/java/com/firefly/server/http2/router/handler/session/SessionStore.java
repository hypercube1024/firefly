package com.firefly.server.http2.router.handler.session;

import javax.servlet.http.HttpSession;

/**
 * @author Pengtao Qiu
 */
public interface SessionStore {

    HttpSession remove(String key);

    HttpSession put(String key, HttpSession value);

    HttpSession get(String key);

    int size();

}
