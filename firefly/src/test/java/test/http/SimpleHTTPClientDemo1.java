package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class SimpleHTTPClientDemo1 {

    public static void main(String[] args) throws Throwable {
        SimpleHTTPClient client = new SimpleHTTPClient();
        final long start = System.currentTimeMillis();
        List<ByteBuffer> list = new ArrayList<>();
        client.get("http://localhost:6656/index")
              .content(list::add)
              .messageComplete((response) -> {
                  long end = System.currentTimeMillis();
                  System.out.println(BufferUtils.toString(list));
                  System.out.println(response.toString());
                  System.out.println(response.getFields());
                  System.out.println("------------------------------------ " + (end - start));
              }).end();

        long s2 = System.currentTimeMillis();
        List<ByteBuffer> list2 = new ArrayList<>();
        client.get("http://localhost:6656/index_1")
              .content(list2::add)
              .messageComplete((response) -> {
                  long end = System.currentTimeMillis();
                  System.out.println(BufferUtils.toString(list2));
                  System.out.println(response.toString());
                  System.out.println(response.getFields());
                  System.out.println("------------------------------------ " + (end - s2));
              }).end();

        long s3 = System.currentTimeMillis();
        Future<SimpleResponse> future = client.get("http://localhost:6656/login").submit();
        SimpleResponse simpleResponse = future.get();
        long end = System.currentTimeMillis();
        System.out.println();
        System.out.println(simpleResponse.getStringBody());
        System.out.println(simpleResponse.toString());
        System.out.println(simpleResponse.getResponse().getFields());
        System.out.println("------------------------------------ " + (end - s3));

        long s4 = System.currentTimeMillis();
        byte[] test = "content=hello_hello".getBytes(StandardCharsets.UTF_8);
        future = client.post("http://localhost:6656/add").output((o) -> {
            try (HTTPOutputStream out = o) {
                out.write(test);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).put(HttpHeader.CONTENT_LENGTH, String.valueOf(test.length))
                       .cookies(simpleResponse.getCookies())
                       .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString())
                       .submit();
        simpleResponse = future.get();
        long end2 = System.currentTimeMillis();
        System.out.println();
        System.out.println(simpleResponse.getStringBody());
        System.out.println(simpleResponse.toString());
        System.out.println(simpleResponse.getResponse().getFields());
        System.out.println("------------------------------------ " + (end2 - s4));

        Thread.sleep(5000);
        client.removeConnectionPool("http://localhost:6656");

        long s5 = System.currentTimeMillis();
        List<ByteBuffer> list3 = new ArrayList<>();
        client.get("http://localhost:6656/index_1")
              .content(list3::add)
              .messageComplete((response) -> {
                  long e5 = System.currentTimeMillis();
                  System.out.println(BufferUtils.toString(list3));
                  System.out.println(response.toString());
                  System.out.println(response.getFields());
                  System.out.println("------------------------------------ " + (e5 - s5));
              }).end();
        Thread.sleep(5000);
        client.stop();
    }

}
