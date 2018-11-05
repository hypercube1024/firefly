package test.net.mapper;

import com.firefly.net.dataformats.Mapper;
import com.firefly.net.tcp.codec.flex.model.Response;

import java.util.HashMap;

/**
 * @author Pengtao Qiu
 */
public class MapperDemo {

    public static void main(String[] args) throws Exception {
        int times = 2_000_000;
        Response response = new Response();
        response.setCode(100);
        response.setMessage("success");
        response.setFields(new HashMap<>());
        response.getFields().put("test", "ok");
        response.getFields().put("version", "v1");

        byte[] data = null;
        Response obj = null;

        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            data = Mapper.getMessagePackMapper().writeValueAsBytes(response);
            obj = Mapper.getMessagePackMapper().readValue(data, Response.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("msg pack: " + (end - start) + ", " + data.length + ", " + obj);

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            data = Mapper.getIonMapper().writeValueAsBytes(response);
            obj = Mapper.getIonMapper().readValue(data, Response.class);
        }
        end = System.currentTimeMillis();
        System.out.println("ion: " + (end - start) + ", " + data.length + ", " + obj);

    }
}
