package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class DirectHTTP2ServerDemo {
    public static void main(String[] args) {
        $.cleartextHTTP2Server().router().post("/cleartextHttp2").handler(ctx -> {
            System.out.println(ctx.getURI().toString());
            System.out.println(ctx.getFields());
            System.out.println(ctx.getStringBody());
            ctx.end("test cleartext http2");
        }).listen("localhost", 2242);
    }
}
