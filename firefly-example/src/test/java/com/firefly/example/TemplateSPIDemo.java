package com.firefly.example;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;

import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class TemplateSPIDemo {
    public static class Example {

        List<Item> items() {
            return Arrays.asList(
                    new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
                    new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
            );
        }

        static class Item {
            Item(String name, String price, List<Feature> features) {
                this.name = name;
                this.price = price;
                this.features = features;
            }

            String name, price;
            List<Feature> features;
        }

        static class Feature {
            Feature(String description) {
                this.description = description;
            }

            String description;
        }

    }

    public static void main(String[] args) {
        $.httpServer().router().get("/example")
         .handler(ctx -> ctx.put(HttpHeader.CONTENT_TYPE, "text/plain")
                            .renderTemplate("template/example.mustache", new Example()))
         .listen("localhost", 8080);
    }
}
