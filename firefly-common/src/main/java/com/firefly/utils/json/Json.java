package com.firefly.utils.json;

import com.firefly.utils.json.io.JsonStringReader;
import com.firefly.utils.json.io.JsonStringWriter;
import com.firefly.utils.json.parser.GeneralJSONObjectStateMacine;
import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.serializer.SerialStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class Json {

    public static String toJson(Object obj) {
        try (JsonWriter writer = new JsonStringWriter()) {
            SerialStateMachine.toJson(obj, writer);
            return writer.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T toObject(String json, Class<T> clazz) {
        try (JsonReader reader = new JsonStringReader(json)) {
            return (T) ParserStateMachine.toObject(reader, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    public static JsonObject toJsonObject(String json) {
        try (JsonReader reader = new JsonStringReader(json)) {
            return GeneralJSONObjectStateMacine.toJsonObject(reader);
        } catch (IOException e) {
            return null;
        }
    }

    public static JsonArray toJsonArray(String json) {
        try (JsonReader reader = new JsonStringReader(json)) {
            return GeneralJSONObjectStateMacine.toJsonArray(reader);
        } catch (IOException e) {
            return null;
        }
    }

}
