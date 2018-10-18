package com.firefly.net.tcp.codec.flex.encode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.firefly.net.tcp.codec.common.msgpack.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public class DefaultMetaInfoGenerator implements MetaInfoGenerator {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    @Override
    public byte[] generate(Object object) {
        try {
            return MessagePackMapper.getMapper().writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("generate msg pack error", e);
            return null;
        }
    }
}
