package test.http.router.handler.body;

import com.firefly.codec.http2.model.MimeTypes;

/**
 * @author Pengtao Qiu
 */
public class TestHTTPBodyHandler {
    public static void main(String[] args) {
//        System.getProperties().entrySet().forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));

        System.out.println(System.getProperty("java.io.tmpdir"));
        System.out.println(MimeTypes.Type.FORM_ENCODED.asString());
    }
}
