package com.firefly.server.http2.router.handler.session;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionHandler extends AbstractSessionHandler {

    public LocalHTTPSessionHandler(HTTPSessionConfiguration configuration) {
        super(configuration);
    }

    @Override
    public SessionStore createSessionStore() {
        return new LocalSessionStore();
    }

}
