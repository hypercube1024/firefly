package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;
import java.util.Date;

public class TimestampSerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        writer.writeLong(((Date) obj).getTime());
    }

}
