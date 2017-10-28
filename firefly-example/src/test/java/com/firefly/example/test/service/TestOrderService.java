package com.firefly.example.test.service;

import com.firefly.example.reactive.coffee.store.dao.InventoryDAO;
import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.service.OrderService;
import com.firefly.example.reactive.coffee.store.service.UserService;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import com.firefly.example.reactive.coffee.store.vo.ProductBuyRequest;
import com.firefly.example.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.firefly.example.reactive.coffee.store.AppMain.javaCtx;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestOrderService extends TestBase {

    private OrderService orderService = javaCtx.getBean(OrderService.class);
    private UserService userService = javaCtx.getBean(UserService.class);
    private InventoryDAO inventoryDAO = javaCtx.getBean(InventoryDAO.class);

    @Test
    public void test() {
        Mono<Boolean> ret = userService.getByName("John").flatMap(user -> {
            ProductBuyRequest request = new ProductBuyRequest();
            request.setUserId(user.getId());
            List<InventoryUpdate> products = new ArrayList<>();
            InventoryUpdate update = new InventoryUpdate();
            update.setAmount(10L);
            update.setProductId(4L);
            products.add(update);

            update = new InventoryUpdate();
            update.setAmount(20L);
            update.setProductId(5L);
            products.add(update);
            request.setProducts(products);
            return orderService.buy(request);
        });
        StepVerifier.create(ret).expectNext(true).expectComplete().verify();

        StepVerifier.create(inventoryDAO.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
            Map<Long, Inventory> map = r.stream().collect(Collectors.toMap(Inventory::getProductId, v -> v));
            Assert.assertThat(map.get(4L).getAmount(), is(67L));
            Assert.assertThat(map.get(5L).getAmount(), is(80L));
        }).expectComplete().verify();
    }

    @Test
    public void testError() {
        Mono<Boolean> ret = userService.getByName("John").flatMap(user -> {
            ProductBuyRequest request = new ProductBuyRequest();
            request.setUserId(user.getId());
            List<InventoryUpdate> products = new ArrayList<>();
            InventoryUpdate update = new InventoryUpdate();
            update.setAmount(90L);
            update.setProductId(4L);
            products.add(update);

            update = new InventoryUpdate();
            update.setAmount(20L);
            update.setProductId(5L);
            products.add(update);
            request.setProducts(products);
            return orderService.buy(request);
        });
        StepVerifier.create(ret)
                    .expectErrorMatches(ex -> ex.getCause() instanceof IllegalStateException && ex.getCause().getMessage().equals("The products are not enough"))
                    .verify();

        StepVerifier.create(inventoryDAO.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
            Map<Long, Inventory> map = r.parallelStream().collect(Collectors.toMap(Inventory::getProductId, v -> v));
            Assert.assertThat(map.get(4L).getAmount(), is(77L));
            Assert.assertThat(map.get(5L).getAmount(), is(100L));
        }).expectComplete().verify();
    }
}
