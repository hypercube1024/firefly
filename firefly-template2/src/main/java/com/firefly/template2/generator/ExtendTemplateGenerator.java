package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import com.firefly.utils.StringUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
public class ExtendTemplateGenerator extends AbstractJavaGenerator<Template2Parser.ExtendTemplateContext> {

    public ExtendTemplateGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void enter(Template2Parser.ExtendTemplateContext node, Writer writer, Object... args) {
        try {
            writer.append(" extends ");
            if (StringUtils.hasText(configuration.getPackagePrefix())) {
                writer.append(configuration.getPackagePrefix()).append('.').append(node.templatePath().getText());
            } else {
                writer.append(node.templatePath().getText());
            }
            generateImplementDeclaration(writer);
        } catch (IOException e) {
            log.error("generate extend template exception", e);
        }
    }

    @Override
    public void exit(Template2Parser.ExtendTemplateContext node, Writer writer, Object... args) {
    }

}
