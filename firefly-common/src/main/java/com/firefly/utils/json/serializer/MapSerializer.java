package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.firefly.utils.json.JsonStringSymbol.*;

public class MapSerializer implements Serializer {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        Map map = (Map) obj;
        Set<Entry<?, ?>> entrySet = map.entrySet();
        if (entrySet.size() == 0) {
            writer.write(EMPTY_ARRAY);
            return;
        }

        writer.append(OBJ_PRE);
        for (Iterator<Entry<?, ?>> it = entrySet.iterator(); ; ) {
            Entry<?, ?> entry = it.next();
            writer.write("\"" + entry.getKey() + "\":");
            SerialStateMachine.toJson(entry.getValue(), writer);
            if (!it.hasNext()) {
                writer.append(OBJ_SUF);
                return;
            }
            writer.append(SEPARATOR);
        }

    }

}
