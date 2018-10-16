package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;

import java.io.IOException;

public class LongArraySerializer extends SimpleArraySerializer {

    public LongArraySerializer(boolean primitive) {
        super(primitive);
    }

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        if (primitive) {
            writer.writeLongArray((long[]) obj);
        } else {
            writer.writeLongArray((Long[]) obj);
        }
    }

}
