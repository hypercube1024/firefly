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
 * Latency   475.91us   78.51us   7.35ms   92.30%
 * Req/Sec     8.40k   389.96     9.37k    76.12%
 * 4016354 requests in 1.00m, 471.13MB read
 * Requests/sec:  66827.27
 * Transfer/sec:      7.84MB
 * <p>
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   495.33us   91.90us   9.41ms   93.92%
 * Req/Sec     8.07k   396.75     9.02k    80.26%
 * 3863290 requests in 1.00m, 1.47GB read
 * Requests/sec:  64281.57
 * Transfer/sec:     25.01MB
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
