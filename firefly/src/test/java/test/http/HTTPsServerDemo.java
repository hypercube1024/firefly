package test.http;

import com.firefly.$;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;

/**
 * @author Pengtao Qiu
 */
public class HTTPsServerDemo {
    public static void main(String[] args) {
        $.httpsServer(new DefaultOpenSSLSecureSessionFactory())
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
