package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.secure.jdk.FileJdkSSLContextFactory;
import com.firefly.net.tcp.secure.jdk.JdkSecureSessionFactory;
import com.firefly.utils.io.ClassPathResource;

import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class JdkFileCertHTTPsServer {
    public static void main(String[] args) throws IOException {
        ClassPathResource pathResource = new ClassPathResource("/fireflySecureKeys.jks");
        SSLContextFactory factory = new FileJdkSSLContextFactory(pathResource.getFile().getAbsolutePath(),
                "123456", "654321");
        $.httpsServer(new JdkSecureSessionFactory(factory, factory))
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
