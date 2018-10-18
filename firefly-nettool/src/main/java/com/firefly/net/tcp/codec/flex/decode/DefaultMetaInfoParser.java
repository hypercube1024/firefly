package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.common.msgpack.MessagePackMapper;
import com.firefly.net.tcp.codec.flex.model.MetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class DefaultMetaInfoParser implements MetaInfoParser {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    @Override
    public <T extends MetaInfo> T parse(byte[] data, Class<T> clazz) {
        try {
            return MessagePackMapper.getMapper().readValue(data, clazz);
        } catch (IOException e) {
            log.error("parse msg pack error", e);
            return null;
        }
    }
}
