package com.firefly.utils.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import static com.firefly.utils.json.parser.GeneralJSONObjectStateMacine.objToJsonArray;
import static com.firefly.utils.json.parser.GeneralJSONObjectStateMacine.objToJsonObject;

public class JsonArray extends ArrayList<Object> {

    private static final long serialVersionUID = -3714987183380639606L;

    public JsonObject getJsonObject(int index) {
        return objToJsonObject(get(index));
    }

    public JsonArray getJsonArray(int index) {
        return objToJsonArray(get(index));
    }

    public String getString(int index) {
        return (String) get(index);
    }

    public byte getByte(int index) {
        return Byte.parseByte(getString(index));
    }

    public short getShort(int index) {
        return Short.parseShort(getString(index));
    }

    public int getInteger(int index) {
        return Integer.parseInt(getString(index));
    }

    public long getLong(int index) {
        return Long.parseLong(getString(index));
    }

    public float getFloat(int index) {
        return Float.parseFloat(getString(index));
    }

    public double getDouble(int index) {
        return Double.parseDouble(getString(index));
    }

    public BigInteger getBigInteger(int index) {
        return new BigInteger(getString(index));
    }

    public BigDecimal getBigDecimal(int index) {
        return new BigDecimal(getString(index));
    }

    public boolean getBoolean(int index) {
        return Boolean.parseBoolean(getString(index));
    }

}
