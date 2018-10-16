package com.firefly.utils.json.support;

import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

import java.io.IOException;
import java.util.Arrays;

public class ParserMetaInfo extends MetaInfo {

    private Class<?> extractedType;
    private Parser parser;

    public void invoke(Object obj, JsonReader reader) {
        try {
            propertyAccess.setValue(obj, getValue(reader));
        } catch (Throwable e) {
            throw new CommonRuntimeException(e);
        }
    }

    public void setPropertyNameString(String propertyNameString) {
        this.propertyNameString = propertyNameString;
        propertyName = propertyNameString.toCharArray();
    }

    public Object getValue(JsonReader reader) throws IOException {
        return parser.convertTo(reader, extractedType);
    }

    public boolean equals(char[] field) {
        return Arrays.equals(propertyName, field);
    }

    public Class<?> getExtractedType() {
        return extractedType;
    }

    public void setExtractedType(Class<?> extractedType) {
        this.extractedType = extractedType;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }
}
