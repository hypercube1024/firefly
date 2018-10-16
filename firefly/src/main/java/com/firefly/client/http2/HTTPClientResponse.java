package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HTTPClientResponse extends MetaData.Response {

    public HTTPClientResponse(HttpVersion version, int status, String reason) {
        super(version, status, reason, new HttpFields(), -1);
    }

    public HTTPClientResponse(HttpVersion version, int status, String reason, HttpFields fields, long contentLength) {
        super(version, status, reason, fields, contentLength);
    }

}
