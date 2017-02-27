package com.firefly.example.http.benchmark;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;

/**
 * ./ab -c15 -n1000000 -k http://127.0.0.1:4455/
 * OS X 10.12.3, java version "1.8.0_111"
 *
 *   Server Software:        Firefly(4.0.21)
     Server Hostname:        127.0.0.1
     Server Port:            4455

     Document Path:          /
     Document Length:        12 bytes

     Concurrency Level:      15
     Time taken for tests:   15.935 seconds
     Complete requests:      1000000
     Failed requests:        0
     Keep-Alive requests:    1000000
     Total transferred:      131000000 bytes
     HTML transferred:       12000000 bytes
     Requests per second:    62754.12 [#/sec] (mean)
     Time per request:       0.239 [ms] (mean)
     Time per request:       0.016 [ms] (mean, across all concurrent requests)
     Transfer rate:          8028.12 [Kbytes/sec] received

     Connection Times (ms)
     min  mean[+/-sd] median   max
     Connect:        0    0   0.0      0       1
     Processing:     0    0   0.1      0       4
     Waiting:        0    0   0.0      0       4
     Total:          0    0   0.1      0       4

     Percentage of the requests served within a certain time (ms)
     50%      0
     66%      0
     75%      0
     80%      0
     90%      0
     95%      0
     98%      0
     99%      0
     100%      4 (longest request)
 *
 * @author Pengtao Qiu
 */
public class HTTPServerPerformance {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/")
         .handler(ctx -> ctx.put(HttpHeader.CONNECTION, "keep-alive")
                            .put(HttpHeader.CONTENT_LENGTH, "12")
                            .end("hello world!"))
         .listen("127.0.0.1", 4455);
    }
}
