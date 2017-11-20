package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.FileCertificateOpenSSLSecureSessionFactory;
import com.firefly.utils.io.ClassPathResource;

import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class OpensslFileCertHTTPsServer {
    public static void main(String[] args) throws IOException {
        ClassPathResource certificate = new ClassPathResource("/myCA.cer");
        ClassPathResource privateKey = new ClassPathResource("/myCAPriv8.key");
        SecureSessionFactory factory = new FileCertificateOpenSSLSecureSessionFactory(
                certificate.getFile().getAbsolutePath(),
                privateKey.getFile().getAbsolutePath());

        $.httpsServer(factory)
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
