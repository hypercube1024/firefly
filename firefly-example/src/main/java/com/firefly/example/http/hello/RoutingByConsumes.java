package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class RoutingByConsumes {

    public static class Car {
        public Long id;
        public String name;
        public String color;

        @Override
        public String toString() {
            return "Car{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", color='" + color + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        $.httpServer()
         .router().put("/product/:id").consumes("*/json")
         .handler(ctx -> {
             String id = ctx.getPathParameter("id");
             String type = ctx.getWildcardMatchedResult(0);
             Car car = ctx.getJsonBody(Car.class);
             ctx.end($.string.replace("Update resource {}: {}. The content type is {}/json", id, car, type));
         })
         .listen(host, port);

        Car car = new Car();
        car.id = 20L;
        car.name = "My car";
        car.color = "black";

        $.httpClient().put($.string.replace("http://{}:{}/product/20", host, port))
         .jsonBody(car)
         .submit()
         .thenAccept(resp -> System.out.println(resp.getStringBody()));
    }
}
