package com.firefly.example.test.dao;

import com.firefly.example.reactive.coffee.store.dao.UserDAO;
import com.firefly.example.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import reactor.test.StepVerifier;

import static com.firefly.example.reactive.coffee.store.AppMain.javaCtx;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestUserDAO extends TestBase {

    private UserDAO userDAO = javaCtx.getBean(UserDAO.class);

    @Test
    public void test() {
        StepVerifier.create(userDAO.getByName("John")).assertNext(user -> {
            Assert.assertThat(user.getId(), is(2L));
            Assert.assertThat(user.getPassword(), is("123456"));
            System.out.println(user);
        }).expectComplete().verify();
    }
}
