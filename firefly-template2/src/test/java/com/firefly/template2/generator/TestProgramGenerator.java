package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

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
        Assert.assertThat(program.generateClassName(templateFile), is("A"));
        System.out.println(program.generatePackageName(templateFile));
        Assert.assertThat(program.generatePackageName(templateFile), is("package com.firefly.template2;" + configuration.getLineSeparator()));

        templateFile = new File(file, "test" + File.separator + "helloTest.fftl");
        System.out.println(templateFile.getAbsolutePath());
        Assert.assertThat(program.generateClassName(templateFile), is("HelloTest"));
        System.out.println(program.generatePackageName(templateFile));
        Assert.assertThat(program.generatePackageName(templateFile), is("package com.firefly.template2.test;" + configuration.getLineSeparator()));

        try (StringWriter writer = new StringWriter()) {
            program.enter(null, writer, templateFile);
            program.exit(null, writer, templateFile);
            System.out.println(writer.toString());
            Assert.assertThat(writer.toString(),
                    is("package com.firefly.template2.test;" + configuration.getLineSeparator()
                            + configuration.getLineSeparator()
                            + "import java.io.OutputStream;" + configuration.getLineSeparator()
                            + "import com.firefly.template2.TemplateRenderer;" + configuration.getLineSeparator()
                            + "import com.firefly.template2.model.VariableStorage;" + configuration.getLineSeparator()
                            + configuration.getLineSeparator()
                            + "public class HelloTest implements TemplateRenderer {" + configuration.getLineSeparator()
                            + "}" + configuration.getLineSeparator()));
        }
    }
}
