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
 * Latency   449.92us   84.63us   5.47ms   92.43%
 * Req/Sec     8.88k   445.93     9.98k    74.94%
 * 4250215 requests in 1.00m, 498.56MB read
 * Requests/sec:  70719.40
 * Transfer/sec:      8.30MB
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 2m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   464.88us   89.56us   5.39ms   93.26%
 * Req/Sec     8.60k   359.89     9.59k    72.21%
 * 8219381 requests in 2.00m, 3.12GB read
 * Requests/sec:  68437.31
 * Transfer/sec:     26.63MB
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
