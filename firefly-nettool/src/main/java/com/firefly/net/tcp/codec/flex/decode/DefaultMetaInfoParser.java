package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.flex.model.MetaInfo;
import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.model.Response;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author Pengtao Qiu
 */
public class DefaultMetaInfoParser implements MetaInfoParser {

    public static final Schema<Request> requestSchema = RuntimeSchema.getSchema(Request.class);
    public static final Schema<Response> responseSchema = RuntimeSchema.getSchema(Response.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T extends MetaInfo> T parse(byte[] data, Class<T> clazz) {
        if (clazz == Request.class) {
            Request req = requestSchema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, req, requestSchema);
            return (T) req;
        } else if (clazz == Response.class) {
            Response resp = responseSchema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, resp, responseSchema);
            return (T) resp;
        } else {
            throw new IllegalArgumentException("The meta info type must be Request or Response");
        }
    }
}
