package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.io.IOException;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class CharacterSerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        writer.append(QUOTE).append((Character) obj).append(QUOTE);
    }

}
