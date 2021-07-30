package com.fireflysource.common.actor;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fireflysource.common.actor.BlockingTask.runBlockingTask;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestActor {

    @Test
    void test() throws Exception {
        StoreActor store = new StoreActor();
        stock(store, "iPhone", 5200, 5);
        stock(store, "h301x", 310, 20);

        List<CompletableFuture<Void>> results = new LinkedList<>();
        results.addAll(purchase(store, "h301x", 400, 10));
        results.addAll(purchase(store, "iPhone", 6500, 9));

        StoreActor.CloseMessage closeMessage = new StoreActor.CloseMessage();
        results.add(closeMessage.todayAmount.thenApply(amount -> {
            System.out.println("Today sales amount: " + amount);
            return null;
        }));
        store.send(closeMessage);

        CompletableFuture.allOf(results.stream().map(r -> r.handle((ignore, throwable) -> {
            Optional.ofNullable(throwable).map(Throwable::getMessage).ifPresent(System.out::println);
            return ignore;
        })).toArray(CompletableFuture[]::new)).join();
        assertEquals(36500L, closeMessage.todayAmount.get());
    }

    private void stock(StoreActor store, String name, long price, int count) {
        IntStream.range(0, count).parallel()
                 .forEach(i -> store.send(new StoreActor.StockMessage(new StoreActor.Product(name, price))));
    }

    private List<CompletableFuture<Void>> purchase(StoreActor store, String name, long price, int count) {
        return IntStream.range(0, count).parallel().boxed().map(i -> {
            StoreActor.PurchaseMessage purchaseMessage = new StoreActor.PurchaseMessage(new StoreActor.Product(name, price));
            store.send(purchaseMessage);
            return purchaseMessage.result.thenAccept(ignore -> System.out.println("purchase " + name + " success."));
        }).collect(Collectors.toList());
    }

    public static class StoreActor extends AbstractAsyncActor<StoreActor.Message> {

        public enum MessageType {
            PURCHASE, STOCK, CLOSE
        }

        public interface Message {
            MessageType getType();
        }

        public static class Product {
            public final String name;
            public final long price;

            public Product(String name, long price) {
                this.name = name;
                this.price = price;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Product product = (Product) o;
                return name.equals(product.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name);
            }
        }

        public static class PurchaseMessage implements Message {

            public final Product product;
            public final CompletableFuture<Void> result = new CompletableFuture<>();

            public PurchaseMessage(Product product) {
                this.product = product;
            }

            @Override
            public MessageType getType() {
                return MessageType.PURCHASE;
            }
        }

        public static class StockMessage implements Message {
            public final Product product;

            public StockMessage(Product product) {
                this.product = product;
            }

            @Override
            public MessageType getType() {
                return MessageType.STOCK;
            }
        }

        public static class CloseMessage implements Message {
            public final CompletableFuture<Long> todayAmount = new CompletableFuture<>();

            @Override
            public MessageType getType() {
                return MessageType.CLOSE;
            }
        }

        private final Map<String, Queue<Product>> products = new HashMap<>();
        private long amount;

        @Override
        public CompletableFuture<Void> onReceiveAsync(Message message) {
            switch (message.getType()) {
                case STOCK:
                    StockMessage stockMessage = (StockMessage) message;
                    return stock(stockMessage.product);
                case PURCHASE:
                    PurchaseMessage purchaseMessage = (PurchaseMessage) message;
                    return purchase(purchaseMessage);
                case CLOSE:
                    shutdown();
                    CloseMessage closeMessage = (CloseMessage) message;
                    closeMessage.todayAmount.complete(amount);
                    return CompletableFuture.completedFuture(null);
                default:
                    return CompletableFuture.completedFuture(null);
            }
        }

        private CompletableFuture<Void> stock(Product product) {
            return CompletableFuture.runAsync(() -> {
                sleep(100);
                products.computeIfAbsent(product.name, k -> new LinkedList<>()).offer(product);
                System.out.println("stock " + product.name + " success.");
            });
        }

        private CompletableFuture<Void> purchase(PurchaseMessage purchaseMessage) {
            return CompletableFuture.runAsync(() -> {
                sleep(200);
                Optional<Product> orderProduct = Optional.ofNullable(products.get(purchaseMessage.product.name)).map(Queue::poll);
                if (orderProduct.isPresent()) {
                    amount += purchaseMessage.product.price;
                    purchaseMessage.result.complete(null);
                } else {
                    purchaseMessage.result.completeExceptionally(new IllegalStateException("The product sells out"));
                }
            });
        }

        private void sleep(long time) {
            runBlockingTask(() -> Thread.sleep(time));
        }

        @Override
        public void onDiscard(Message message) {
            System.out.println(Thread.currentThread().getName() + " -- discard message: " + message.getType());
            if (message.getType() == MessageType.PURCHASE) {
                PurchaseMessage purchaseMessage = (PurchaseMessage) message;
                purchaseMessage.result.completeExceptionally(new IllegalStateException("The store is close"));
            }
        }
    }
}
