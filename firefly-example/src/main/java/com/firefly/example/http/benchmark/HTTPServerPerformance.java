package com.firefly.example.http.benchmark;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;

import java.util.Arrays;
import java.util.List;

/**
 * OS X 10.13, java version "1.8.0_144"
 * log level: INFO
 * JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/
 * Running 1m test @ http://127.0.0.1:4455/
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   457.43us  167.19us  21.41ms   97.88%
 * Req/Sec     8.79k   592.76    10.20k    76.00%
 * 4205803 requests in 1.00m, 493.35MB read
 * Requests/sec:  69977.88
 * Transfer/sec:      8.21MB
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   475.17us   90.50us  10.45ms   94.61%
 * Req/Sec     8.41k   336.80     9.41k    75.08%
 * 4022717 requests in 1.00m, 1.53GB read
 * Requests/sec:  66934.11
 * Transfer/sec:     26.04MB
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
