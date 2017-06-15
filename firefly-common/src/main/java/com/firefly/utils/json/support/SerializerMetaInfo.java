package com.firefly.utils.json.support;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;

import static com.firefly.utils.json.JsonStringSymbol.OBJ_SEPARATOR;
import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

public class SerializerMetaInfo extends MetaInfo {

    private Serializer serializer;

    public void setPropertyName(String propertyName, boolean first) {
        propertyNameString = propertyName;
        this.propertyName = ((first ? "" : ",") + QUOTE + propertyName + QUOTE + OBJ_SEPARATOR).toCharArray();
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public void toJson(Object obj, JsonWriter writer)
            throws IOException {
        Object ret = propertyAccess.getValue(obj);
        if (ret == null) {
            writer.writeNull();
        } else {
            serializer.convertTo(writer, ret);
        }
    }

}
