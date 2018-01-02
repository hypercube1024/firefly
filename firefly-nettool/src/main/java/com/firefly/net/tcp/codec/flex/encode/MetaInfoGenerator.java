package com.firefly.net.tcp.codec.flex.encode;

import com.firefly.utils.ServiceUtils;

/**
 * @author Pengtao Qiu
 */
public interface MetaInfoGenerator {

    MetaInfoGenerator DEFAULT = ServiceUtils.loadService(MetaInfoGenerator.class, new DefaultMetaInfoGenerator());

    byte[] generate(Object object);
}
