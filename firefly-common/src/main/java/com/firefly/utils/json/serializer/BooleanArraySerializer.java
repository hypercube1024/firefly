package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;

import java.io.IOException;

public class BooleanArraySerializer extends SimpleArraySerializer {

    public BooleanArraySerializer(boolean primitive) {
        super(primitive);
    }

    @Override
    public void convertTo(JsonWriter writer, Object obj)
            throws IOException {
        if (primitive) {
            writer.writeBooleanArray((boolean[]) obj);
        } else {
            writer.writeBooleanArray((Boolean[]) obj);
        }
    }

}
