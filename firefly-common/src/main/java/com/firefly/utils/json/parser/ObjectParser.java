package com.firefly.utils.json.parser;

import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ParserMetaInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ObjectParser implements Parser {

    private ParserMetaInfo[] parserMetaInfos;
    private int max;
    private Map<String, ParserMetaInfo> map;
    private boolean useMap;

    public void init(Class<?> clazz, Type type) {
        parserMetaInfos = DecodeCompiler.compile(clazz, type);
        max = parserMetaInfos.length - 1;
        if (max >= 8) {
            map = new HashMap<>();
            for (ParserMetaInfo parserMetaInfo : parserMetaInfos) {
                map.put(parserMetaInfo.getPropertyNameString(), parserMetaInfo);
            }
            useMap = true;
        }
    }

    @Override
    public Object convertTo(JsonReader reader, Class<?> clazz) {
        if (reader.isNull())
            return null;

        if (!reader.isObject())
            throw new JsonException("json string is not object format");

        Object obj;
        try {
            obj = clazz.newInstance();
        } catch (Throwable e) {
            throw new CommonRuntimeException(e);
        }

        if (reader.isEmptyObject())
            return obj;

        for (int i = 0; ; i++) {
            ParserMetaInfo parser = parserMetaInfos[i];
            char[] field = reader.readField(parser.getPropertyName());
            if (!reader.isColon())
                throw new JsonException("missing ':'");

            if (field == null) { // the same order，skip
                parser.invoke(obj, reader);
            } else {
                ParserMetaInfo np = find(field);
                if (np != null)
                    np.invoke(obj, reader);
                else
                    reader.skipValue();
            }

            if (i == max)
                break;

            char ch = reader.readAndSkipBlank();
            if (ch == '}') // if JSON string fields is less than the meta information，end reading
                return obj;

            if (ch != ',')
                throw new JsonException("missing ','");
        }

        char ch = reader.readAndSkipBlank();
        if (ch == '}')
            return obj;

        if (ch != ',')
            throw new JsonException("json string is not object format");

        for (; ; ) { // if JSON string fields is more than the meta information, continue reading
            char[] field = reader.readChars();
            if (!reader.isColon())
                throw new JsonException("missing ':'");

            ParserMetaInfo np = find(field);
            if (np != null)
                np.invoke(obj, reader);
            else
                reader.skipValue();

            char c = reader.readAndSkipBlank();
            if (c == '}') // the object end symbol
                return obj;


            if (c != ',')
                throw new JsonException("missing ','");
        }
    }

    private ParserMetaInfo find(char[] field) {
        if (useMap) {
            return map.get(new String(field));
        } else {
            for (ParserMetaInfo parserMetaInfo : parserMetaInfos) {
                if (parserMetaInfo.equals(field))
                    return parserMetaInfo;
            }
        }
        return null;
    }

}
