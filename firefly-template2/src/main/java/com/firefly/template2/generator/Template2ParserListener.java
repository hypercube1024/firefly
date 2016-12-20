package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2BaseListener;
import com.firefly.template2.parser.Template2Parser;
import com.firefly.template2.parser.Template2ParserWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author Pengtao Qiu
 */
public class Template2ParserListener extends Template2BaseListener {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private final Configuration configuration;
    private final Generator generator;
    private final Template2ParserWrap template2ParserWrap;
    private final Writer writer;
    private final File outputJavaFile;
    private final String className;

    public Template2ParserListener(Configuration configuration, Template2ParserWrap template2ParserWrap) throws IOException {
        this.configuration = configuration;
        this.template2ParserWrap = template2ParserWrap;
        this.generator = new Generator(configuration);

        File root = configuration.getRootPath();
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new IOException("create root directory exception");
            }
        }
        ProgramGenerator programGenerator = generator.getGenerator(Template2Parser.ProgramContext.class);
        className = programGenerator.generatePackage(template2ParserWrap.getFile()) + "." + programGenerator.generateClass(template2ParserWrap.getFile());
        String outputFilePath = File.separatorChar + className.replace('.', File.separatorChar) + ".java";
        outputJavaFile = new File(root, outputFilePath);
        if (!outputJavaFile.getParentFile().exists()) {
            if (!outputJavaFile.getParentFile().mkdirs()) {
                throw new IOException("create package directory exception");
            }
        }
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputJavaFile), configuration.getOutputJavaFileCharset()));
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public File getOutputJavaFile() {
        return outputJavaFile;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public void enterProgram(Template2Parser.ProgramContext ctx) {
        ProgramGenerator programGenerator = generator.getGenerator(Template2Parser.ProgramContext.class);
        programGenerator.enter(ctx, writer, template2ParserWrap.getFile());
    }

    @Override
    public void exitProgram(Template2Parser.ProgramContext ctx) {
        ProgramGenerator programGenerator = generator.getGenerator(Template2Parser.ProgramContext.class);
        programGenerator.exit(ctx, writer);
        try {
            writer.close();
        } catch (IOException e) {
            log.error("exit program exception", e);
        }
    }

    @Override
    public void enterExtendTemplate(Template2Parser.ExtendTemplateContext ctx) {
        ExtendTemplateGenerator extendTemplateGenerator = generator.getGenerator(Template2Parser.ExtendTemplateContext.class);
        extendTemplateGenerator.enter(ctx, writer);
    }

    @Override
    public void exitExtendTemplate(Template2Parser.ExtendTemplateContext ctx) {
        ExtendTemplateGenerator extendTemplateGenerator = generator.getGenerator(Template2Parser.ExtendTemplateContext.class);
        extendTemplateGenerator.exit(ctx, writer);
    }
}
