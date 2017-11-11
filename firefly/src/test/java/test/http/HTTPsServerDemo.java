package test.http;

import com.firefly.$;
import com.firefly.net.tcp.secure.SelfSignedCertificateOpenSSLSecureSessionFactory;

/**
 * @author Pengtao Qiu
 */
public class HTTPsServerDemo {
    public static void main(String[] args) {
        $.httpsServer(new SelfSignedCertificateOpenSSLSecureSessionFactory())
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
