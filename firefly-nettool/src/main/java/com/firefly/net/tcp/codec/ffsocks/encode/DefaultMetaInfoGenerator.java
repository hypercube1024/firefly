package com.firefly.net.tcp.codec.ffsocks.encode;

import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.model.Response;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author Pengtao Qiu
 */
public class DefaultMetaInfoGenerator implements MetaInfoGenerator {

    public static final Schema<Request> requestSchema = RuntimeSchema.getSchema(Request.class);
    public static final Schema<Response> responseSchema = RuntimeSchema.getSchema(Response.class);

    @Override
    public byte[] generate(Object object) {
        byte[] data;
        if (object instanceof Request) {
            LinkedBuffer buffer = LinkedBuffer.allocate();
            try {
                data = ProtostuffIOUtil.toByteArray((Request) object, requestSchema, buffer);
            } finally {
                buffer.clear();
            }
        } else if (object instanceof Response) {
            LinkedBuffer buffer = LinkedBuffer.allocate();
            try {
                data = ProtostuffIOUtil.toByteArray((Response) object, responseSchema, buffer);
            } finally {
                buffer.clear();
            }
        } else {
            throw new IllegalArgumentException("The meta info type must be Request or Response");
        }
        return data;
    }
}
