package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;

public class DynamicObjectSerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        if (obj.getClass().equals(Object.class)) {
            writer.writeNull();
        } else {
            SerialStateMachine.toJson(obj, writer);
        }
    }

}
