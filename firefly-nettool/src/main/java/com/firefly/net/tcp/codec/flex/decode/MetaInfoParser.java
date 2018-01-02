package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.flex.model.MetaInfo;
import com.firefly.utils.ServiceUtils;

/**
 * @author Pengtao Qiu
 */
public interface MetaInfoParser {

    MetaInfoParser DEFAULT = ServiceUtils.loadService(MetaInfoParser.class, new DefaultMetaInfoParser());

    <T extends MetaInfo> T parse(byte[] data, Class<T> clazz);
}
