package com.firefly.template2;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestTemplate2Factory {

    @Test
    public void testOutputString() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setTemplateHome(new File(Template2Compiler.class.getResource("/").toURI()).getAbsolutePath());
        Template2Factory factory = new Template2Factory(configuration);
        Assert.assertThat(factory.render("TestMain", new HashMap<>()), is(
                "hello" + configuration.getLineSeparator() +
                        "bar" + configuration.getLineSeparator() +
                        "ok" + configuration.getLineSeparator() +
                        "foo" + configuration.getLineSeparator() +
                        configuration.getLineSeparator() +
                        "end"));
    }

}
