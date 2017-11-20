package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;

/**
 * @author Pengtao Qiu
 */
public class OpensslHTTPsServer {
    public static void main(String[] args) {
        $.httpsServer(new DefaultOpenSSLSecureSessionFactory())
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
