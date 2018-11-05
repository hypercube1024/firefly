package com.firefly.net.dataformats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author Pengtao Qiu
 */
abstract public class Mapper {

    private static class MessagePackMapperHolder {
        static final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    private static class IonMapperHolder {
        static final ObjectMapper objectMapper = new IonObjectMapper();
    }

    public static ObjectMapper getMessagePackMapper() {
        return MessagePackMapperHolder.objectMapper;
    }

    public static ObjectMapper getIonMapper() {
        return IonMapperHolder.objectMapper;
    }

}
