package com.firefly.utils.json.compiler;

import com.firefly.utils.BeanUtils;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.parser.ComplexTypeParser;
import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.support.ParserMetaInfo;
import com.firefly.utils.lang.bean.PropertyAccess;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.firefly.utils.json.support.PropertyUtils.getDateFormat;
import static com.firefly.utils.json.support.PropertyUtils.isTransientField;

public class DecodeCompiler {
    private static final ParserMetaInfo[] EMPTY_ARRAY = new ParserMetaInfo[0];

    public static ParserMetaInfo[] compile(Class<?> clazz, Type type) {
        Set<ParserMetaInfo> fieldSet = new TreeSet<>();
        for (Map.Entry<String, PropertyAccess> entry : BeanUtils.getBeanAccessByType(type).entrySet()) {
            String propertyName = entry.getKey();
            PropertyAccess propertyAccess = entry.getValue();
            Method getter = propertyAccess.getGetterMethod();
            Method setter = propertyAccess.getSetterMethod();

            if (isTransientField(propertyName, clazz, setter, getter)) continue;

            ParserMetaInfo parserMetaInfo = new ParserMetaInfo();
            parserMetaInfo.setPropertyNameString(propertyName);
            parserMetaInfo.setPropertyAccess(propertyAccess);

            Class<?> extractedType = propertyAccess.extractClass();
            if (Collection.class.isAssignableFrom(extractedType)) {
                parserMetaInfo.setExtractedType(ComplexTypeParser.getImplClass(extractedType));
                parserMetaInfo.setParser(ParserStateMachine.getParser(extractedType, propertyAccess.getType(), null));
            } else if (Map.class.isAssignableFrom(extractedType)) {
                parserMetaInfo.setExtractedType(ComplexTypeParser.getImplClass(extractedType));
                parserMetaInfo.setParser(ParserStateMachine.getParser(extractedType, propertyAccess.getType(), null));
            } else {
                parserMetaInfo.setExtractedType(extractedType);
                parserMetaInfo.setParser(ParserStateMachine.getParser(extractedType, propertyAccess.getType(), getDateFormat(propertyName, clazz, setter, getter)));
            }
            fieldSet.add(parserMetaInfo);
        }

        ParserMetaInfo[] parserMetaInfos = fieldSet.toArray(EMPTY_ARRAY);
        if (parserMetaInfos.length <= 0) {
            throw new JsonException("not support the " + clazz.getName());
        }
        return parserMetaInfos;
    }


}
