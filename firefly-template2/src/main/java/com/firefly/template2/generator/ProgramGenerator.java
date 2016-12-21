package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import com.firefly.template2.utils.PathUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

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
            writer.append("package ").append(generatePackage(templateFile)).append(";").append(configuration.getLineSeparator());
            writer.append(generateImport());
            writer.append("public class ").append(generateClass(templateFile));
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
        }
    }

    @Override
    public void exit(Template2Parser.ProgramContext node, Writer writer, Object... args) {
        List<String> list = (List<String>) args[0];
        try {
            for (String s : list) {
                writer.append("    ").append(s);
            }
            writer.append('}').append(configuration.getLineSeparator());
        } catch (IOException e) {
            log.error("generate program exception", e);
        }
    }

    public String generateImport() {
        StringBuilder s = new StringBuilder();
        s.append(configuration.getLineSeparator());
        s.append("import java.io.OutputStream;").append(configuration.getLineSeparator());
        s.append("import java.io.IOException;").append(configuration.getLineSeparator());
        s.append("import com.firefly.template2.TemplateRenderer;").append(configuration.getLineSeparator());
        s.append("import com.firefly.template2.model.VariableStorage;").append(configuration.getLineSeparator());
        s.append(configuration.getLineSeparator());
        return s.toString();
    }

    public String generateClass(File templateFile) {
        String name = templateFile.getName();
        name = name.substring(0, name.length() - configuration.getSuffix().length() - 1);
        if (name.length() == 1) {
            return name.toUpperCase();
        } else {
            return "" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    public String generatePackage(File templateFile) {
        StringBuilder ret = new StringBuilder();
        ret.append(configuration.getPackagePrefix());
        String templateFilePath = templateFile.getAbsolutePath();
        String packageName = templateFilePath.substring(configuration.getTemplateHome().length(), templateFilePath.length() - templateFile.getName().length());
        if (!packageName.equals(File.separator)) {
            ret.append(PathUtils.removeTheLastPathSeparator(packageName).replace(File.separatorChar, '.'));
        }
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
