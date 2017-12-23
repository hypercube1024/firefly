package com.firefly.net.tcp.codec.model;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Pengtao Qiu
 */
public class MetaInfo {
    protected int contentType;
    protected Map<String, String> fields;
    protected Supplier<Map<String, String>> trailer;

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public Supplier<Map<String, String>> getTrailer() {
        return trailer;
    }

    public void setTrailer(Supplier<Map<String, String>> trailer) {
        this.trailer = trailer;
    }
}
