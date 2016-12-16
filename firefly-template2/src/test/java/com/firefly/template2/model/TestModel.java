package com.firefly.template2.model;

import com.firefly.template2.model.impl.ModelServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestModel {

    @Test
    public void test() {
        ModelService service = new ModelServiceImpl();
        Map<String, Object> global = service.createMap();
        global.put("hello", "world");
        global.put("level", "1");
        Assert.assertThat(service.size(), is(1));

        Map<String, Object> local = service.createMap();
        local.put("hello", "local variable");
        Assert.assertThat(service.size(), is(2));

        Assert.assertThat(service.get("hello"), is("local variable"));
        Assert.assertThat(service.get("level"), is("1"));
        service.popMap();
        Assert.assertThat(service.size(), is(1));
    }

    @Test
    public void testCallAction() {
        Map<String, Object> global = new HashMap<>();
        global.put("hello", "world");
        global.put("level", "1");

        ModelService service = new ModelServiceImpl(global);

        service.callAction(s -> {
            Assert.assertThat(service.size(), is(2));
            s.put("hello", "local variable");
            Assert.assertThat(service.size(), is(2));
            Assert.assertThat(service.get("hello"), is("local variable"));
            Assert.assertThat(service.get("level"), is("1"));
        });
        Assert.assertThat(service.size(), is(1));
    }
}
