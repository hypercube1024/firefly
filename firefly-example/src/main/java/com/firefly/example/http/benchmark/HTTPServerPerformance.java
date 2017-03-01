package com.firefly.example.http.benchmark;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;

/**
 * OS X 10.12.3, java version "1.8.0_111"
 * log level: INFO
 * JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
 *
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/
 * Running 1m test @ http://127.0.0.1:4455/
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   555.32us   88.19us   6.72ms   87.13%
 * Req/Sec     7.21k   494.12     8.30k    76.91%
 * 3449340 requests in 1.00m, 411.19MB read
 * Requests/sec:  57393.23
 * Transfer/sec:      6.84MB
 *
 * @author Pengtao Qiu
 */
public class HTTPServerPerformance {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/")
         .handler(ctx -> ctx.end("hello world!"))
         .listen("127.0.0.1", 4455);
    }
}
