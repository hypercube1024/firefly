package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;

import java.io.IOException;

public class ShortArraySerializer extends SimpleArraySerializer {

    public ShortArraySerializer(boolean primitive) {
        super(primitive);
    }

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        if (primitive) {
            writer.writeShortArray((short[]) obj);
        } else {
            writer.writeShortArray((Short[]) obj);
        }

    }

}
