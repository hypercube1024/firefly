package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestProgramGenerator {

    @Test
    public void test() throws URISyntaxException, IOException {
        Configuration configuration = new Configuration();
        File file = new File(Configuration.class.getResource("/").toURI());
        configuration.setTemplateHome(file.getAbsolutePath());
        System.out.println(configuration.getTemplateHome());

        Generator generator = new Generator(configuration);
        ProgramGenerator program = generator.getGenerator(Template2Parser.ProgramContext.class);

        File templateFile = new File(file, "a.fftl");
        System.out.println(templateFile.getName());
        Assert.assertThat(program.generateClass(templateFile), is("A"));
        System.out.println(program.generatePackage(templateFile));
        Assert.assertThat(program.generatePackage(templateFile), is(configuration.getPackagePrefix()));

        templateFile = new File(file, "test" + File.separator + "helloTest.fftl");
        System.out.println(templateFile.getAbsolutePath());
        Assert.assertThat(program.generateClass(templateFile), is("HelloTest"));
        System.out.println(program.generatePackage(templateFile));
        Assert.assertThat(program.generatePackage(templateFile), is(configuration.getPackagePrefix() + ".test"));

        try (StringWriter writer = new StringWriter()) {
            program.enter(null, writer, templateFile);
            program.exit(null, writer, new ArrayList<String>());
            System.out.println(writer.toString());
            Assert.assertThat(writer.toString(),
                    is("package com.firefly.template2.compiled.test;" + configuration.getLineSeparator()
                            + configuration.getLineSeparator()
                            + "import java.io.OutputStream;" + configuration.getLineSeparator()
                            + "import java.io.IOException;" + configuration.getLineSeparator()
                            + "import com.firefly.template2.TemplateRenderer;" + configuration.getLineSeparator()
                            + "import com.firefly.template2.model.VariableStorage;" + configuration.getLineSeparator()
                            + configuration.getLineSeparator()
                            + "public class HelloTest implements TemplateRenderer {" + configuration.getLineSeparator()
                            + "}" + configuration.getLineSeparator()));
        }
    }
}
