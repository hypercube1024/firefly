package com.firefly.codec.http2.model;

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
        this(header, header.asString(), valueString, value);
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