package com.fireflysource.net.http.common.v2.hpack;

import com.fireflysource.net.http.common.model.HttpField;
import com.fireflysource.net.http.common.model.HttpHeader;

public class StaticTableHttpField extends HttpField {
    private final Object value;

    public StaticTableHttpField(HttpHeader header, String name,
                                String valueString, Object value) {
        super(header, name, valueString);
        if (value == null)
            throw new IllegalArgumentException();
        this.value = value;
    }

    public StaticTableHttpField(HttpHeader header, String valueString,
                                Object value) {
        this(header, header.getValue(), valueString, value);
    }

    public StaticTableHttpField(String name, String valueString, Object value) {
        super(name, valueString);
        if (value == null)
            throw new IllegalArgumentException();
        this.value = value;
    }

    public Object getStaticValue() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + "(evaluated)";
    }
}