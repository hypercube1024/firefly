package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.model.*;
import com.firefly.utils.Assert;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class PredefinedHTTP1Response {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    public static final byte[] H2C_BYTES;
    public static final byte[] CONTINUE_100_BYTES;

    static {
        MetaData.Response H2C_RESPONSE = new MetaData.Response(HttpVersion.HTTP_1_1, 101, new HttpFields());
        H2C_RESPONSE.getFields().put(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE);
        H2C_RESPONSE.getFields().put(HttpHeader.UPGRADE, "h2c");

        try {
            ByteBuffer header = BufferUtils.allocate(128);
            HttpGenerator gen = new HttpGenerator(true, true);
            HttpGenerator.Result result = gen.generateResponse(H2C_RESPONSE, false, header, null, null, true);
            Assert.state(result == HttpGenerator.Result.FLUSH && gen.getState() == HttpGenerator.State.COMPLETING,
                    "generate h2c bytes error");
            H2C_BYTES = BufferUtils.toArray(header);

            header = BufferUtils.allocate(128);
            gen = new HttpGenerator(true, true);
            result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, false, header, null, null, false);
            Assert.state(result == HttpGenerator.Result.FLUSH && gen.getState() == HttpGenerator.State.COMPLETING_1XX,
                    "generate continue 100 error");
            CONTINUE_100_BYTES = BufferUtils.toArray(header);
        } catch (IOException e) {
            log.error("generate h2c response exception", e);
            throw new CommonRuntimeException(e);
        }
    }
}
