package com.firefly.utils.json.compiler;

import com.firefly.utils.BeanUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.json.annotation.JsonProperty;
import com.firefly.utils.json.serializer.SerialStateMachine;
import com.firefly.utils.json.support.SerializerMetaInfo;
import com.firefly.utils.lang.bean.PropertyAccess;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.firefly.utils.json.support.PropertyUtils.*;

public class EncodeCompiler {

    private static final SerializerMetaInfo[] EMPTY_ARRAY = new SerializerMetaInfo[0];

    public static SerializerMetaInfo[] compile(Class<?> clazz) {
        Set<SerializerMetaInfo> fieldSet = new TreeSet<>();
        for (Map.Entry<String, PropertyAccess> entry : BeanUtils.getBeanAccess(clazz).entrySet()) {
            String propertyName = entry.getKey();
            PropertyAccess propertyAccess = entry.getValue();
            Method getter = propertyAccess.getGetterMethod();
            Method setter = propertyAccess.getSetterMethod();

            if (isTransientField(propertyName, clazz, setter, getter)) continue;

            Class<?> propertyClass = null;
            if (propertyAccess.getGetterMethod() != null) {
                propertyClass = propertyAccess.getGetterMethod().getReturnType();
            } else if (propertyAccess.getField() != null) {
                propertyClass = propertyAccess.getField().getType();
            }

            if (propertyClass == null) continue;

            SerializerMetaInfo fieldMetaInfo = new SerializerMetaInfo();
            fieldMetaInfo.setPropertyAccess(propertyAccess);
            String jsonPropertyName = Optional.ofNullable(getJsonProperty(propertyName, clazz, setter, getter))
                                              .map(JsonProperty::value)
                                              .filter(StringUtils::hasText)
                                              .orElse(propertyName);
            fieldMetaInfo.setPropertyName(jsonPropertyName, false);
            fieldMetaInfo.setSerializer(SerialStateMachine.getSerializer(propertyClass, getDateFormat(propertyName, clazz, setter, getter)));
            fieldSet.add(fieldMetaInfo);
        }

        SerializerMetaInfo[] serializerMetaInfos = fieldSet.toArray(EMPTY_ARRAY);
        if (serializerMetaInfos.length > 0) {
            serializerMetaInfos[0].setPropertyName(serializerMetaInfos[0].getPropertyNameString(), true);
        }
        return serializerMetaInfos;
    }

}
