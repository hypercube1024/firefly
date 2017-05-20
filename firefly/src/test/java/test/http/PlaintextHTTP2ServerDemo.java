package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class PlaintextHTTP2ServerDemo {
    public static void main(String[] args) {
        $.plaintextHTTP2Server().router().post("/plaintextHttp2").handler(ctx -> {
            System.out.println(ctx.getURI().toString());
            System.out.println(ctx.getFields());
            System.out.println(ctx.getStringBody());
            ctx.end("test plaintext http2");
        }).listen("localhost", 2242);
    }
}
