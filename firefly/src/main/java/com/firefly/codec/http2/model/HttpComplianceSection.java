package com.firefly.codec.http2.model;

public enum HttpComplianceSection {

    CASE_INSENSITIVE_FIELD_VALUE_CACHE("", "Use case insensitive field value cache"),
    METHOD_CASE_SENSITIVE("https://tools.ietf.org/html/rfc7230#section-3.1.1", "Method is case-sensitive"),
    FIELD_COLON("https://tools.ietf.org/html/rfc7230#section-3.2", "Fields must have a Colon"),
    FIELD_NAME_CASE_INSENSITIVE("https://tools.ietf.org/html/rfc7230#section-3.2", "Field name is case-insensitive"),
    NO_WS_AFTER_FIELD_NAME("https://tools.ietf.org/html/rfc7230#section-3.2.4", "Whitespace not allowed after field name"),
    NO_FIELD_FOLDING("https://tools.ietf.org/html/rfc7230#section-3.2.4", "No line Folding"),
    NO_HTTP_0_9("https://tools.ietf.org/html/rfc7230#appendix-A.2", "No HTTP/0.9"),
    TRANSFER_ENCODING_WITH_CONTENT_LENGTH("https://tools.ietf.org/html/rfc7230#section-3.3.1", "Transfer-Encoding and Content-Length"),
    MULTIPLE_CONTENT_LENGTHS("https://tools.ietf.org/html/rfc7230#section-3.3.1", "Multiple Content-Lengths");

    public final String url;
    public final String description;

    HttpComplianceSection(String url, String description) {
        this.url = url;
        this.description = description;
    }

    public String getURL() {
        return url;
    }

    public String getDescription() {
        return description;
    }

}
