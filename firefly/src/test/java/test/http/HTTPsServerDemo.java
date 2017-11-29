package test.http;

import com.firefly.$;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pengtao Qiu
 */
public class HTTPsServerDemo {
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(HTTPsServerDemo.class.getResource("/").toURI());

        $.httpsServer(new DefaultOpenSSLSecureSessionFactory())
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .router().get("/static/*")
         .handler(new StaticFileHandler(path.toAbsolutePath().toString()))
         .listen("localhost", 8081);
    }
}
