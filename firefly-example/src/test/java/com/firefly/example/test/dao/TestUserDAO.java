package com.firefly.example.test.dao;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.dao.UserDAO;
import org.junit.Assert;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestUserDAO extends TestBase {

    private UserDAO userDAO = $.getBean(UserDAO.class);

    @Test
    public void test() {
        StepVerifier.create(userDAO.getByName("John")).assertNext(user -> {
            Assert.assertThat(user.getId(), is(2L));
            Assert.assertThat(user.getPassword(), is("123456"));
            System.out.println(user);
        }).expectComplete().verify();
    }
}
