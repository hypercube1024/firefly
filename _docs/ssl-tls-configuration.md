---

category : docs
title: SSL/TLS configuration

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Enable HTTPs](#enable-https)
- [Set secure session factory](#set-secure-session-factory)

<!-- /TOC -->

# Enable HTTPs
```kotlin
fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx -> ctx.end("Hello https! ") }
        .enableSecureConnection() // (1)
        .listen("localhost", 8090)

    `$`.httpClient().get("https://localhost:8090/").submit() // (2)
        .thenAccept { response -> println(response.stringBody) }
}
```
1. Use `httpServer.enableSecureConnection` method to enable default https certificate. 
2. Once the https enabled, the http2 protocol has the high priority in the ALPN handshake.  

# Set secure session factory
The https server uses Firefly insecure self-signed certificate. It is just can be used in test or development stage. If you want to use your certificate file instead of the self-signed certificate, you can use the `FileConscryptSSLContextFactory` that can read your jks(KeyStore) file.

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
