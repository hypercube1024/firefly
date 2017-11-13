---

category : docs
layout: document
title: SSL/TLS configuration

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Enable HTTPs](#enable-https)
- [ALPN configuration](#alpn-configuration)
- [Set secure session factory](#set-secure-session-factory)
- [Use the OpenSSL engine](#use-the-openssl-engine)

<!-- /TOC -->

# Enable HTTPs
Firefly provides JDK SSL engine and OpenSSL engine. The JDK SSL engine is the default.
The Java8 SSL engine does not support ALPN (Application Layer Protocol Negotiation).  

The HTTP/2 need ALPN within a TLS handshake. If you use the Java8 SSL engine, you need to add a VM option to set up Jetty ALPN boot jar.  

If you can not modify the VM options in some situations, you can use the OpenSSL engine that supports ALPN, and it need not add any VM options.  

The first, we create a simple example to demonstrate how to enable HTTPs:
```java
public class HelloHTTPsServerAndClient {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(2);

        HTTP2ServerBuilder httpServer = $.httpsServer();
        httpServer.router().get("/").handler(ctx -> ctx.write("hello world! ").next())
                  .router().get("/").handler(ctx -> ctx.end("end message"))
                  .listen("localhost", 8081);

        $.httpsClient().get("https://localhost:8081/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()))
         .thenAccept(res -> phaser.arrive());

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpsClient().stop();
    }
}
```

Run it. The console shows:
```
hello world! end message
```

# ALPN configuration
The `$.httpsServer()` and `$.httpsClient()` use the default SSL/TLS configuration. It does not enable HTTP2 protocol. If you want to enable HTTP2, the first you must download the `alpn-boot.jar` from the central repository.

Be certain to get the [ALPN Boot Jar version](https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions) which matches the version of your JRE. Such as, our JDK version is 1.8.0u144, the matched `alpn-boot.jar` version is 8.1.11.v20170118.    

Add the VM options:
```
-Xbootclasspath/p:<path_to_alpn_boot_jar>
```

Run the first case. Then you can search the `firefly-system` log:
```
INFO 2017-11-13 16:15:03 firefly-system firefly-aio-tcp-client-1 -> Session 0 handshake success. The application protocol is h2
```
It represents the HTTP2 is enabled.

# Set secure session factory
The HTTPs server uses Firefly insecure self-signed certificate. It is just can be used in test or development stage. If you want to use your certificate file instead of the self-signed certificate, you can use the `FileJdkSSLContextFactory` that can read your jks(KeyStore) file.

The first, copy your jks file (such as `fireflySecureKeys.jks`) to your classpath.

Notes: the maven resource filter will modify your jks file. It can cause the jks verifying failure. If you use maven and copy the jks file to the `src/main/resources` folder, you must exclude the jks file in maven resource filter.  For example:
```xml
<resources>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
        <includes>
            <include>**/*.xml</include>
        </includes>
    </resource>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>false</filtering>
        <excludes>
            <exclude>**/*.xml</exclude>
        </excludes>
    </resource>
</resources>
```
We only add xml files to the maven resource filter.

The second, set the `FileJdkSSLContextFactory`. For example:
```java
public class JdkFileCertHTTPsServer {
    public static void main(String[] args) throws IOException {
        ClassPathResource pathResource = new ClassPathResource("/fireflySecureKeys.jks");
        SSLContextFactory factory = new FileJdkSSLContextFactory(
        pathResource.getFile().getAbsolutePath(),"123456", "654321");

        $.httpsServer(new JdkSecureSessionFactory(factory, factory))
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
```

# Use the OpenSSL engine
If you can not modify the VM options in some situations, we recommend you use the OpenSSL engine. For example:
```java
public class OpensslHTTPsServer {
    public static void main(String[] args) {
        $.httpsServer(new DefaultOpenSSLSecureSessionFactory())
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}
```
Run it and visit the `https://localhost:8081`. The browser will render
```
hello world!
```

You can also use your own openssl certificate. For example:
```java
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
```

Notes: the OpenSSL private key must be `PKCS8` format. You can use the tool `openssl pkcs8` to convert format. Such as:
```
openssl genrsa -out myCA.key 2048
openssl req -new -x509 -key myCA.key -out myCA.cer -days 36500
openssl pkcs8 -topk8 -inform PEM -outform PEM -in myCA.key -out myCAPriv8.key -nocrypt
```
