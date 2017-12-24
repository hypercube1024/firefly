package test.net.tcp.codec.ffsocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.utils.function.Func0;
import com.firefly.utils.function.Func1;
import com.firefly.utils.json.Json;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Pengtao Qiu
 */
public class MetaInfoBenchmark {

    public static final IonObjectMapper mapper = new IonObjectMapper();
    public static Schema<Request> schema;

    static {
        mapper.setCreateBinaryWriters(true);
        schema = RuntimeSchema.getSchema(Request.class);
    }

    public static void main(String[] args) {
        test();
    }

    public static void test() {
        Request request = new Request();
        request.setPath("ffsocks://hello");
        request.setFields(new HashMap<>());
        for (int i = 0; i < 10; i++) {
            request.getFields().put("myKey" + i, "value" + i);
        }

        benchmark("Protostuff", () -> {
            LinkedBuffer buffer = LinkedBuffer.allocate();
            try {
                return ProtostuffIOUtil.toByteArray(request, schema, buffer);
            } finally {
                buffer.clear();
            }
        }, data -> {
            Request req = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, req, schema);
            return req;
        });

        benchmark("ION", () -> {
            try {
                return mapper.writeValueAsBytes(request);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }, data -> {
            try {
                return mapper.readValue(data, Request.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        benchmark("JSON", () -> Json.toJson(request).getBytes(StandardCharsets.UTF_8),
                data -> Json.toObject(new String(data, StandardCharsets.UTF_8), Request.class));
    }

    public static void benchmark(String format, Func0<byte[]> serialize, Func1<byte[], Request> deserialize) {
        int loop = 10 * 1000 * 1000;
        long start = System.currentTimeMillis();
        byte[] data = null;
        for (int i = 0; i < loop; i++) {
            data = serialize.call();
        }
        long end = System.currentTimeMillis();
        System.out.println(format + " serialization. elapsed: " + (end - start) + ", size: " + data.length);

        start = System.currentTimeMillis();
        Request request = null;
        for (int i = 0; i < loop; i++) {
            request = deserialize.call(data);
        }
        end = System.currentTimeMillis();
        System.out.println(format + " deserialization. elapsed: " + (end - start) + ", " + request);
    }
}
