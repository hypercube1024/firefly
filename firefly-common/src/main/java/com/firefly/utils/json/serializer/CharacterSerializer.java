package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

public class CharacterSerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        writer.append(QUOTE).append((Character) obj).append(QUOTE);
    }

}
