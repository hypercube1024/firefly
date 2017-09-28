package com.firefly.example.reactive.coffee.store.service.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.InventoryDAO;
import com.firefly.example.reactive.coffee.store.dao.OrderDAO;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Order;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.service.OrderService;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import com.firefly.example.reactive.coffee.store.vo.OrderStatus;
import com.firefly.example.reactive.coffee.store.vo.ProductBuyRequest;
import com.firefly.reactive.adapter.db.ReactiveSQLClient;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import com.firefly.utils.CollectionUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
@Component
public class OrderServiceImpl implements OrderService {

    @Inject
    private OrderDAO orderDAO;

    @Inject
    private InventoryDAO inventoryDAO;

    @Inject
    private ProductDAO productDAO;

    @Inject
    private ReactiveSQLClient db;


    @Override
    public Mono<Boolean> buy(ProductBuyRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("The product request is required"));
        }

        if (request.getUserId() == null || request.getUserId().equals(0L)) {
            return Mono.error(new IllegalArgumentException("The user id is required"));
        }

        if (CollectionUtils.isEmpty(request.getProducts())) {
            return Mono.error(new IllegalArgumentException("The products must bu not empty"));
        }

        return db.newTransaction(c -> buy(request, c));
    }

    private Mono<Boolean> buy(ProductBuyRequest request, ReactiveSQLConnection c) {
        return inventoryDAO.updateBatch(request.getProducts(), InventoryOperator.SUB, c)
                           .doOnSuccess(this::verifyInventory)
                           .then(arr -> productDAO.list(toProductIdList(request), c))
                           .map(products -> toOrders(request, products))
                           .then(orders -> orderDAO.insertBatch(orders, c))
                           .map(orderIdList -> true);
    }

    private void verifyInventory(int[] arr) {
        if (Arrays.stream(arr).anyMatch(i -> i <= 0)) {
            throw new IllegalStateException("The products are not enough");
        }
    }

    private List<Long> toProductIdList(ProductBuyRequest request) {
        return request.getProducts().parallelStream().map(InventoryUpdate::getProductId).collect(Collectors.toList());
    }

    private List<Order> toOrders(ProductBuyRequest request, List<Product> products) {
        return products.parallelStream().map(product -> {
            Long amount = getAmount(request, product);
            double totalPrice = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(amount)).doubleValue();

            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setStatus(OrderStatus.FINISHED.getValue());
            order.setAmount(amount);
            order.setPrice(product.getPrice());
            order.setTotalPrice(totalPrice);
            order.setProductId(product.getId());
            order.setDescription(product.getDescription());
            return order;
        }).collect(Collectors.toList());
    }

    private Long getAmount(ProductBuyRequest request, Product product) {
        return request.getProducts()
                      .parallelStream()
                      .filter(i -> i.getProductId().equals(product.getId()))
                      .map(InventoryUpdate::getAmount)
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException("The product amounts must be more than 0"));
    }
}
