package com.firefly.template2.model;

import com.firefly.template2.model.impl.VariableStorageImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Pengtao Qiu
 */
public class TestVariableStorage {

    @Test
    public void testCallAction() {
        Map<String, Object> global = new HashMap<>();
        global.put("hello", "world");
        global.put("level", "1");

        Map<String, Object> args = new HashMap<>();
        args.put("methodName", "main");

        VariableStorage var = new VariableStorageImpl(Collections.singletonList(global));

        var.callAction(() -> {
            Assert.assertThat(var.size(), is(3));

            var.put("hello", "local variable");
            Assert.assertThat(var.size(), is(3));

            Assert.assertThat(var.get("hello"), is("local variable"));
            Assert.assertThat(var.getFirst("hello"), is("local variable"));
            Assert.assertThat(var.get("level"), is("1"));
            Assert.assertThat(var.getFirst("level"), nullValue());
            Assert.assertThat(var.get("methodName"), is("main"));

            var.callAction(() -> {
                Assert.assertThat(var.size(), is(4));
                var.put("count", 1);
                Assert.assertThat(var.get("count"), is(1));
            });
        }, args);
        Assert.assertThat(var.size(), is(1));
    }
}
