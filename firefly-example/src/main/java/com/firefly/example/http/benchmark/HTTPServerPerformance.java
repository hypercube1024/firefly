package com.firefly.example.http.benchmark;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;

import java.util.Arrays;
import java.util.List;

/**
 * OS X 10.12.3, java version "1.8.0_111"
 * log level: INFO
 * JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/
 * Running 1m test @ http://127.0.0.1:4455/
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   555.32us   88.19us   6.72ms   87.13%
 * Req/Sec     7.21k   494.12     8.30k    76.91%
 * 3449340 requests in 1.00m, 411.19MB read
 * Requests/sec:  57393.23
 * Transfer/sec:      6.84MB
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   565.51us   84.67us   5.20ms   85.52%
 * Req/Sec     7.08k   443.70     8.08k    79.91%
 * 3386095 requests in 1.00m, 1.29GB read
 * Requests/sec:  56341.23
 * Transfer/sec:     22.03MB
 *
 * @author Pengtao Qiu
 */
public class HTTPServerPerformance {
    public static class ItemRepository {

        String name;

        public ItemRepository(String name) {
            this.name = name;
        }

        List<Item> repository() {
            return Arrays.asList(new Item("foo", 33.2),
                    new Item("beer", 5.99),
                    new Item("cola", 2.5),
                    new Item("mineral water", 2));
        }

        static class Item {
            String name;
            double price;

            public Item(String name, double price) {
                this.name = name;
                this.price = price;
            }
        }
    }

    public static void main(String[] args) {
        $.httpServer()
         .router().get("/")
         .handler(ctx -> ctx.end("hello world!"))
         .router().get("/items")
         .handler(ctx -> ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML_UTF_8.asString())
                            .renderTemplate("template/benchmark/items.mustache", new ItemRepository("drinks")))
         .listen("127.0.0.1", 4455);
    }
}
