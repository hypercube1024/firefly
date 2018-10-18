package com.firefly.net.tcp.codec.common.msgpack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @author Pengtao Qiu
 */
abstract public class MessagePackMapper {

    private static class Holder {
        static final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
    }

    public static ObjectMapper getMapper() {
        return Holder.objectMapper;
    }
}
