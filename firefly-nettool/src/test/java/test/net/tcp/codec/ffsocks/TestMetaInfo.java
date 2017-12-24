package test.net.tcp.codec.ffsocks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import com.firefly.net.tcp.codec.ffsocks.model.ClientRequest;
import com.firefly.utils.json.Json;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class TestMetaInfo {

    public static final IonObjectMapper mapper = new IonObjectMapper();
    public static final IonObjectMapper binMapper = new IonObjectMapper();

    static {
        binMapper.setCreateBinaryWriters(true);
    }

    @Test
    public void test() throws Exception {
        ClientRequest<Map<String, String>> clientRequest = new ClientRequest<>();
        clientRequest.setPath("ffsocks://hello");
        clientRequest.setFields(new HashMap<>());
        clientRequest.getFields().put("Host", "127.0.0.1");
        clientRequest.setData(new HashMap<>());
        for (int i = 0; i < 10; i++) {
            clientRequest.getData().put("myTest" + i, "xxx" + i);
        }

        byte[] encoded = mapper.writeValueAsBytes(clientRequest);
        System.out.println("text: " + encoded.length);
        System.out.println(new String(encoded));

        String json = Json.toJson(clientRequest);
        System.out.println("json text: " + json.getBytes().length);
        System.out.println(json);

        ClientRequest<Map<String, String>> request = mapper.readValue(encoded, new TypeReference<ClientRequest<Map<String, String>>>() {
        });
        System.out.println(request);

        encoded = binMapper.writeValueAsBytes(clientRequest);
        System.out.println("bin: " + encoded.length);

        request = binMapper.readValue(encoded, new TypeReference<ClientRequest<Map<String, String>>>() {
        });
        System.out.println(request);

    }
}
