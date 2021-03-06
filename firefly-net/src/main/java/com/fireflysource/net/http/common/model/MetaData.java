package com.fireflysource.net.http.common.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

public class MetaData implements Iterable<HttpField> {
    private final HttpFields fields;
    private HttpVersion httpVersion;
    private long contentLength;
    private Supplier<HttpFields> trailers;
    private boolean onlyTrailer;

    public MetaData(HttpVersion version, HttpFields fields) {
        this(version, fields, Long.MIN_VALUE);
    }

    public MetaData(HttpVersion version, HttpFields fields, long contentLength) {
        httpVersion = version;
        this.fields = fields;
        this.contentLength = contentLength < 0 ? Long.MIN_VALUE : contentLength;
    }

    protected void recycle() {
        httpVersion = null;
        if (fields != null) {
            fields.clear();
        }
        contentLength = Long.MIN_VALUE;
        trailers = null;
    }

    public boolean isRequest() {
        return false;
    }

    public boolean isResponse() {
        return false;
    }

    /**
     * @return the HTTP version of this MetaData object
     */
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * @param httpVersion the HTTP version to set
     */
    public void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * @return the HTTP fields of this MetaData object
     */
    public HttpFields getFields() {
        return fields;
    }

    public Supplier<HttpFields> getTrailerSupplier() {
        return trailers;
    }

    public boolean isOnlyTrailer() {
        return onlyTrailer;
    }

    public void setOnlyTrailer(boolean onlyTrailer) {
        this.onlyTrailer = onlyTrailer;
    }

    public void setTrailerSupplier(Supplier<HttpFields> trailers) {
        this.trailers = trailers;
    }

    /**
     * @return the content length if available, otherwise {@link Long#MIN_VALUE}
     */
    public long getContentLength() {
        if (contentLength == Long.MIN_VALUE) {
            return Optional.ofNullable(fields.getField(HttpHeader.CONTENT_LENGTH))
                           .map(HttpField::getValue)
                           .map(Long::parseLong)
                           .orElse(Long.MIN_VALUE);
        } else {
            return contentLength;
        }
    }

    /**
     * @return an iterator over the HTTP fields
     * @see #getFields()
     */
    public Iterator<HttpField> iterator() {
        HttpFields fields = getFields();
        return fields == null ? Collections.emptyIterator() : fields.iterator();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (HttpField field : this)
            out.append(field).append(System.lineSeparator());
        return out.toString();
    }

    public static class Request extends MetaData {
        private String method;
        private HttpURI uri;

        public Request(HttpFields fields) {
            this(null, null, null, fields);
        }

        public Request(String method, HttpURI uri, HttpVersion version, HttpFields fields) {
            this(method, uri, version, fields, Long.MIN_VALUE);
        }

        public Request(String method, HttpURI uri, HttpVersion version, HttpFields fields, long contentLength) {
            super(version, fields, contentLength);
            this.method = method;
            this.uri = uri;
        }

        public Request(String method, HttpScheme scheme, HostPortHttpField hostPort, String uri, HttpVersion version, HttpFields fields) {
            this(method, new HttpURI(scheme == null ? null : scheme.getValue(), hostPort.getHost(), hostPort.getPort(), uri), version, fields);
        }

        public Request(String method, HttpScheme scheme, HostPortHttpField hostPort, String uri, HttpVersion version, HttpFields fields, long contentLength) {
            this(method, new HttpURI(scheme == null ? null : scheme.getValue(), hostPort.getHost(), hostPort.getPort(), uri), version, fields, contentLength);
        }

        public Request(String method, String scheme, HostPortHttpField hostPort, String uri, HttpVersion version, HttpFields fields, long contentLength) {
            this(method, new HttpURI(scheme, hostPort.getHost(), hostPort.getPort(), uri), version, fields, contentLength);
        }

        public Request(Request request) {
            super(request.getHttpVersion(), new HttpFields(request.getFields()), request.getContentLength());
            this.method = request.getMethod();
            this.uri = new HttpURI(request.getURI());
        }

        public void recycle() {
            super.recycle();
            method = null;
            if (uri != null)
                uri.clear();
        }

        @Override
        public boolean isRequest() {
            return true;
        }

        /**
         * @return the HTTP method
         */
        public String getMethod() {
            return method;
        }

        /**
         * @param method the HTTP method to set
         */
        public void setMethod(String method) {
            this.method = method;
        }

        /**
         * @return the HTTP URI
         */
        public HttpURI getURI() {
            return uri;
        }

        /**
         * @param uri the HTTP URI to set
         */
        public void setURI(HttpURI uri) {
            this.uri = uri;
        }

        /**
         * @return the HTTP URI in string form
         */
        public String getURIString() {
            return uri == null ? null : uri.toString();
        }

        @Override
        public String toString() {
            HttpFields fields = getFields();
            return String.format("%s{u=%s,%s,h=%d,cl=%d}",
                    getMethod(), getURI(), getHttpVersion(), fields == null ? -1 : fields.size(), getContentLength());
        }
    }

    public static class Response extends MetaData {
        private int status;
        private String reason;

        public Response() {
            this(null, 0, null);
        }

        public Response(HttpFields httpFields) {
            this(null, 0, httpFields);
        }

        public Response(HttpVersion version, int status, HttpFields fields) {
            this(version, status, fields, Long.MIN_VALUE);
        }

        public Response(HttpVersion version, int status, HttpFields fields, long contentLength) {
            super(version, fields, contentLength);
            this.status = status;
        }

        public Response(HttpVersion version, int status, String reason, HttpFields fields, long contentLength) {
            super(version, fields, contentLength);
            this.reason = reason;
            this.status = status;
        }

        public Response(Response response) {
            super(response.getHttpVersion(), new HttpFields(response.getFields()), response.getContentLength());
            this.reason = response.reason;
            this.status = response.status;
        }

        @Override
        public boolean isResponse() {
            return true;
        }

        /**
         * @return the HTTP status
         */
        public int getStatus() {
            return status;
        }

        /**
         * @param status the HTTP status to set
         */
        public void setStatus(int status) {
            this.status = status;
        }

        /**
         * @return the HTTP reason
         */
        public String getReason() {
            return reason;
        }

        /**
         * @param reason the HTTP reason to set
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        public void recycle() {
            super.recycle();
            reason = null;
            status = 0;
        }

        @Override
        public String toString() {
            HttpFields fields = getFields();
            return String.format("%s{s=%d,h=%d,cl=%d}", getHttpVersion(), getStatus(), fields == null ? -1 : fields.size(), getContentLength());
        }
    }
}
