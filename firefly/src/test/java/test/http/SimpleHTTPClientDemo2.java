package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.codec.http2.stream.HTTPOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SimpleHTTPClientDemo2 {

    public static void main(String[] args) throws Throwable {
        SimpleHTTPClientConfiguration config = new SimpleHTTPClientConfiguration();
        config.setSecureConnectionEnabled(true);
        SimpleHTTPClient client = new SimpleHTTPClient(config);

        long start = System.currentTimeMillis();
        client.get("https://localhost:6655/index")
              .submit()
              .thenApply(res -> res.getStringBody("UTF-8"))
              .thenAccept(System.out::println)
              .thenAccept(v -> System.out.println("--------------- " + (System.currentTimeMillis() - start)));

        client.get("https://localhost:6655/index_1").submit()
              .thenApply(res -> res.getStringBody("UTF-8"))
              .thenAccept(System.out::println)
              .thenAccept(v -> System.out.println("--------------- " + (System.currentTimeMillis() - start)));


        SimpleResponse simpleResponse = client.get("https://localhost:6655/login").submit().get();
        long end = System.currentTimeMillis();
        System.out.println();
        System.out.println(simpleResponse.getStringBody());
        System.out.println(simpleResponse.toString());
        System.out.println(simpleResponse.getResponse().getFields());
        System.out.println("------------------------------------ " + (end - start));

        long s2 = System.currentTimeMillis();
        byte[] test = "content=hello_hello".getBytes(StandardCharsets.UTF_8);
        client.post("http://localhost:6655/add")
              .output((o) -> {
                  try (HTTPOutputStream out = o) {
                      out.write(test);
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              })
              .put(HttpHeader.CONTENT_LENGTH, String.valueOf(test.length))
              .cookies(simpleResponse.getCookies())
              .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString())
              .submit()
              .thenAccept(res -> {
                  System.out.println();
                  System.out.println(simpleResponse.getStringBody());
                  System.out.println(simpleResponse.toString());
                  System.out.println(simpleResponse.getResponse().getFields());
                  System.out.println("------------------------------------ " + (System.currentTimeMillis() - s2));
              });


        Thread.sleep(5000);
        client.stop();
    }

}
