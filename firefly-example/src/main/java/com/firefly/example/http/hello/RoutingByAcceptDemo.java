package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;

import static com.firefly.example.http.hello.RoutingByConsumes.Car;

/**
 * @author Pengtao Qiu
 */
public class RoutingByAcceptDemo {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().put("/product/:id").consumes("*/json").produces("text/plain")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String type = ctx.getWildcardMatchedResult(0);
             Car car = ctx.getJsonBody(Car.class);
             ctx.end($.string.replace("Update resource {}: {}. The content type is {}/json", id, car, type));
         })
         .router().put("/product/:id").consumes("*/json").produces("application/json")
         .handler(ctx -> {
             Car car = ctx.getJsonBody(Car.class);
             ctx.writeJson(car).end();
         })
         .listen(host, port);

        Car car = new Car();
        car.id = 20L;
        car.name = "My car";
        car.color = "black";

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
