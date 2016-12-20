package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.Template2Compiler;
import com.firefly.utils.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestMainFunctionGenerator {

    @Test
    public void test() throws IOException, URISyntaxException {
        Configuration configuration = new Configuration();
        configuration.setTemplateHome(new File(Template2Compiler.class.getResource("/").toURI()).getAbsolutePath());
        Template2Compiler compiler = new Template2Compiler(configuration);
        compiler.generateJavaFiles().compileJavaFiles();

        File file = compiler.getJavaFiles().get(configuration.getPackagePrefix() + ".TestMain");
        System.out.println(FileUtils.readFileToString(file, configuration.getOutputJavaFileCharset()));
        Assert.assertThat(FileUtils.readFileToString(file, configuration.getOutputJavaFileCharset()), is(
                "package com.firefly.template2.compiled;" + configuration.getLineSeparator() +
                        configuration.getLineSeparator() +
                        "import java.io.OutputStream;" + configuration.getLineSeparator() +
                        "import com.firefly.template2.TemplateRenderer;" + configuration.getLineSeparator() +
                        "import com.firefly.template2.model.VariableStorage;" + configuration.getLineSeparator() +
                        configuration.getLineSeparator() +
                        "public class TestMain implements TemplateRenderer {" + configuration.getLineSeparator() +
                        "    public void main(OutputStream outputStream, VariableStorage var) {" + configuration.getLineSeparator() +
                        "    }" + configuration.getLineSeparator() +
                        "}" + configuration.getLineSeparator()));
    }
}
