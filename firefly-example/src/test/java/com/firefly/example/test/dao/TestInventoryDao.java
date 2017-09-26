package com.firefly.example.test.dao;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.dao.InventoryDao;
import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import org.junit.Assert;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestInventoryDao extends TestBase {

    private InventoryDao inventoryDao = $.getBean(InventoryDao.class);

    @Test
    public void testUpdate() {
        List<InventoryUpdate> list = new ArrayList<>();
        InventoryUpdate update = new InventoryUpdate();
        update.setAmount(10L);
        update.setProductId(4L);
        list.add(update);

        update = new InventoryUpdate();
        update.setAmount(20L);
        update.setProductId(5L);
        list.add(update);

        StepVerifier.create(inventoryDao.updateBatch(list, InventoryOperator.SUB)).assertNext(r -> {
            Assert.assertThat(r.length, is(2));
            Arrays.stream(r).forEach(row -> Assert.assertThat(row, is(1)));
        }).expectComplete().verify();

        StepVerifier.create(inventoryDao.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
            Map<Long, Inventory> map = r.parallelStream().collect(Collectors.toMap(Inventory::getProductId, v -> v));
            Assert.assertThat(map.get(4L).getAmount(), is(67L));
            Assert.assertThat(map.get(5L).getAmount(), is(80L));
        }).expectComplete().verify();

        list = new ArrayList<>();
        update = new InventoryUpdate();
        update.setAmount(90L);
        update.setProductId(4L);
        list.add(update);

        update = new InventoryUpdate();
        update.setAmount(20L);
        update.setProductId(5L);
        list.add(update);

        StepVerifier.create(inventoryDao.updateBatch(list, InventoryOperator.SUB)).assertNext(r -> {
            Assert.assertThat(r.length, is(2));
            Assert.assertThat(r[0], is(0));
            Assert.assertThat(r[1], is(1));
        }).expectComplete().verify();

        StepVerifier.create(inventoryDao.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
            Map<Long, Inventory> map = r.parallelStream().collect(Collectors.toMap(Inventory::getProductId, v -> v));
            Assert.assertThat(map.get(4L).getAmount(), is(67L));
            Assert.assertThat(map.get(5L).getAmount(), is(60L));
        }).expectComplete().verify();
    }
}