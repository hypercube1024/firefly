package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class PlaintextHTTP2ClientDemo {
    public static void main(String[] args) {
        $.plaintextHTTP2Client()
         .post("http://localhost:2242/plaintextHttp2")
         .body("post data")
         .submit().thenAccept(res -> {
            System.out.println(res.getStatus() + " " + res.getReason() + " " + res.getHttpVersion().asString());
            System.out.println(res.getFields());
            System.out.println(res.getStringBody());
        });
    }
}
