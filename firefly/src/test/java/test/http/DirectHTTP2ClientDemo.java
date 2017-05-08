package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class DirectHTTP2ClientDemo {
    public static void main(String[] args) {
        $.cleartextHTTP2Client()
         .post("http://localhost:2242/cleartextHttp2")
         .body("post data")
         .submit().thenAccept(res -> {
            System.out.println(res.getStatus() + " " + res.getReason() + " " + res.getHttpVersion().asString());
            System.out.println(res.getFields());
            System.out.println(res.getStringBody());
        });
    }
}
