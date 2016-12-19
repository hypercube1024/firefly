package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import com.firefly.template2.utils.PathUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
public class ProgramGenerator extends AbstractJavaGenerator<Template2Parser.ProgramContext> {


    public ProgramGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void enter(Template2Parser.ProgramContext node, Writer writer, Object... args) {
        check(args);
        File templateFile = (File) args[0];

        try {
            writer.append(generatePackageName(templateFile));
            writer.append(configuration.getLineSeparator());
            generateImport(writer);
            writer.append(configuration.getLineSeparator());
            writer.append("public class ").append(generateClassName(templateFile));
            if (node != null) {
                Template2Parser.ExtendTemplateContext extendTemplateContext = node.getRuleContext(Template2Parser.ExtendTemplateContext.class, 0);
                if (extendTemplateContext == null) {
                    generateImplementDeclaration(writer);
                }
            } else {
                generateImplementDeclaration(writer);
            }
        } catch (IOException e) {
            log.error("generate program exception", e);
            throw new RuntimeException("generate program exception", e);
        }
    }

    public void generateImplementDeclaration(Writer writer) throws IOException {
        writer.append(" implements TemplateRenderer {").append(configuration.getLineSeparator());
    }

    public void generateImport(Writer writer) throws IOException {
        writer.append("import java.io.OutputStream;").append(configuration.getLineSeparator());
        writer.append("import com.firefly.template2.TemplateRenderer;").append(configuration.getLineSeparator());
        writer.append("import com.firefly.template2.model.VariableStorage;").append(configuration.getLineSeparator());
    }

    public String generateClassName(File templateFile) {
        String name = templateFile.getName();
        name = name.substring(0, name.length() - configuration.getSuffix().length() - 1);
        if (name.length() == 1) {
            return name.toUpperCase();
        } else {
            return "" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    public String generatePackageName(File templateFile) {
        StringBuilder ret = new StringBuilder();
        ret.append("package ");
        ret.append(configuration.getPackagePrefix());

        String templateFilePath = templateFile.getAbsolutePath();
        String packageName = templateFilePath.substring(configuration.getTemplateHome().length(), templateFilePath.length() - templateFile.getName().length());
        if (!packageName.equals(File.separator)) {
            ret.append(PathUtils.removeTheLastPathSeparator(packageName).replace(File.separatorChar, '.'));
        }
        ret.append(";").append(configuration.getLineSeparator());
        return ret.toString();
    }

    private void check(Object... args) {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("the argument is empty");
        }

        if (!(args[0] instanceof File)) {
            throw new IllegalArgumentException("the first argument is not template file");
        }
    }

}
