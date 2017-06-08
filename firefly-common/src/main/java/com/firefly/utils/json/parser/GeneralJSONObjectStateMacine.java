package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.exception.JsonException;

import java.io.IOException;

abstract public class GeneralJSONObjectStateMacine {

    public static JsonObject toJsonObject(final JsonReader reader) throws IOException {
        if (!reader.isObject()) {
            throw new JsonException("It is not a JSON object, the position is " + reader.position());
        }

        JsonObject map = new JsonObject();

        if (reader.isEmptyObject()) {
            return map;
        }

        fieldLoop:
        while (true) {
            char[] field = reader.readChars();
            if (!reader.isColon()) {
                throw new JsonException("The error occur, near by the key \"" + String.valueOf(field) + "\", the position is " + reader.position());
            }

            char ch = reader.readAndSkipBlank();
            reader.decreasePosition();

            switch (ch) {
                case '{':
                    map.put(String.valueOf(field), toJsonObject(reader));
                    break;
                case '[':
                    map.put(String.valueOf(field), toJsonArray(reader));
                    break;
                case '"':
                    map.put(String.valueOf(field), reader.readString());
                    break;
                default:
                    map.put(String.valueOf(field), reader.readValueAsString());
                    break;
            }

            char fieldEndCh = reader.readAndSkipBlank();
            switch (fieldEndCh) {
                case '}':
                    break fieldLoop;
                case ',':
                    break;
                default:
                    throw new JsonException("The error is at position " + reader.position());
            }
        }
        return map;
    }

    public static JsonArray toJsonArray(final JsonReader reader) throws IOException {
        if (!reader.isArray()) {
            throw new JsonException("It is not a JSON array, the position is " + reader.position());
        }

        JsonArray array = new JsonArray();

        if (reader.isEmptyArray()) {
            return array;
        }

        arrayElementLoop:
        while (true) {
            char ch = reader.readAndSkipBlank();
            reader.decreasePosition();

            switch (ch) {
                case '{':
                    array.add(toJsonObject(reader));
                    break;
                case '[':
                    array.add(toJsonArray(reader));
                    break;
                case '"':
                    array.add(reader.readString());
                    break;
                default:
                    array.add(reader.readValueAsString());
                    break;
            }

            char fieldEndCh = reader.readAndSkipBlank();
            switch (fieldEndCh) {
                case ']':
                    break arrayElementLoop;
                case ',':
                    break;
                default:
                    throw new JsonException("The error is at position " + reader.position());
            }
        }
        return array;
    }

    public static JsonObject objToJsonObject(Object obj) {
        if (obj instanceof String && "null".equals(obj)) {
            return null;
        } else if (obj instanceof JsonObject) {
            return (JsonObject) obj;
        } else {
            throw new JsonException("json type error");
        }
    }

    public static JsonArray objToJsonArray(Object obj) {
        if (obj instanceof String && "null".equals(obj)) {
            return null;
        } else if (obj instanceof JsonArray) {
            return (JsonArray) obj;
        } else {
            throw new JsonException("json type error");
        }
    }
}
