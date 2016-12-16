package com.firefly.template2.model;

import com.firefly.template2.model.impl.VariableStorageImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestVariableStorage {

    @Test
    public void test() {
        VariableStorage var = new VariableStorageImpl();
        Map<String, Object> global = var.createVariable();
        global.put("hello", "world");
        global.put("level", "1");
        Assert.assertThat(var.size(), is(1));

        Map<String, Object> local = var.createVariable();
        local.put("hello", "local variable");
        Assert.assertThat(var.size(), is(2));

        Assert.assertThat(var.get("hello"), is("local variable"));
        Assert.assertThat(var.get("level"), is("1"));
        var.removeVariable();
        Assert.assertThat(var.size(), is(1));
    }

    @Test
    public void testCallAction() {
        Map<String, Object> global = new HashMap<>();
        global.put("hello", "world");
        global.put("level", "1");

        Map<String, Object> params = new HashMap<>();
        params.put("methodName", "main");

        VariableStorage var = new VariableStorageImpl(Arrays.asList(global, params));

        var.callAction(() -> {
            Assert.assertThat(var.size(), is(3));

            var.put("hello", "local variable");
            Assert.assertThat(var.size(), is(3));

            Assert.assertThat(var.get("hello"), is("local variable"));
            Assert.assertThat(var.get("level"), is("1"));
            Assert.assertThat(var.get("methodName"), is("main"));

            var.callAction(() -> {
                Assert.assertThat(var.size(), is(4));
                var.put("count", 1);
                Assert.assertThat(var.get("count"), is(1));
            });
        });
        Assert.assertThat(var.size(), is(2));
    }
}
