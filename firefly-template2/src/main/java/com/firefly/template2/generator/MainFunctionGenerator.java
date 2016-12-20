package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
public class MainFunctionGenerator extends AbstractJavaGenerator<Template2Parser.MainFunctionContext> {

    public MainFunctionGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void enter(Template2Parser.MainFunctionContext node, Writer writer, Object... args) {
        try {
            generateBlank(writer, args);
            writer.append("public void main(OutputStream out, VariableStorage var) throws IOException {")
                  .append(configuration.getLineSeparator());
        } catch (IOException e) {
            log.error("generate main function exception", e);
        }

    }
}
