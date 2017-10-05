package com.firefly.example.test.dao;

import com.firefly.example.reactive.coffee.store.dao.InventoryDAO;
import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import com.firefly.example.test.TestBase;
import com.firefly.reactive.adapter.db.ReactiveSQLClient;
import org.junit.Assert;
import org.junit.Test;
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
public class TestInventoryDAO extends TestBase {

    private ReactiveSQLClient db = javaCtx.getBean(ReactiveSQLClient.class);
    private InventoryDAO inventoryDAO = javaCtx.getBean(InventoryDAO.class);

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

        List<InventoryUpdate> update1 = list;
        StepVerifier.create(db.newTransaction(c -> inventoryDAO.updateBatch(update1, InventoryOperator.SUB, c))).assertNext(r -> {
            Assert.assertThat(r.length, is(2));
            Arrays.stream(r).forEach(row -> Assert.assertThat(row, is(1)));
        }).expectComplete().verify();

        StepVerifier.create(inventoryDAO.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
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

        List<InventoryUpdate> update2 = list;
        StepVerifier.create(db.newTransaction(c -> inventoryDAO.updateBatch(update2, InventoryOperator.SUB, c))).assertNext(r -> {
            Assert.assertThat(r.length, is(2));
            Assert.assertThat(r[0], is(0));
            Assert.assertThat(r[1], is(1));
        }).expectComplete().verify();

        StepVerifier.create(inventoryDAO.listByProductId(Arrays.asList(4L, 5L))).assertNext(r -> {
            Map<Long, Inventory> map = r.parallelStream().collect(Collectors.toMap(Inventory::getProductId, v -> v));
            Assert.assertThat(map.get(4L).getAmount(), is(67L));
            Assert.assertThat(map.get(5L).getAmount(), is(60L));
        }).expectComplete().verify();
    }
}
