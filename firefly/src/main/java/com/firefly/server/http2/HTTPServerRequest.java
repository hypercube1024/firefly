package com.firefly.server.http2;

import com.firefly.codec.http2.model.*;

public class HTTPServerRequest extends MetaData.Request {

    public HTTPServerRequest(String method, String uri, HttpVersion version) {
        super(method, new HttpURI(HttpMethod.fromString(method) == HttpMethod.CONNECT ? "http://" + uri : uri), version, new HttpFields());
    }
}
