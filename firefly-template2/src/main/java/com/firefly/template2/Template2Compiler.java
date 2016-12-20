package com.firefly.template2;

import com.firefly.template2.generator.Template2ParserListener;
import com.firefly.template2.parser.Template2ParserHelper;
import com.firefly.template2.parser.Template2ParserWrap;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class Template2Compiler {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private Configuration configuration;
    private final Map<String, File> javaFiles = new HashMap<>();

    public Template2Compiler() {
    }

    public Template2Compiler(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Map<String, File> getJavaFiles() {
        return javaFiles;
    }

    public void generateJavaFile() {
        Path path = Paths.get(configuration.getTemplateHome());
        try {
            Files.walk(path)
                 .filter(p -> p.getFileName().toString().endsWith("." + configuration.getSuffix()))
                 .forEach(p -> {
                     if (log.isDebugEnabled()) {
                         log.debug("find template file -> {}", p.toFile().getName());
                     }

                     Template2ParserHelper helper = new Template2ParserHelper(configuration);
                     Template2ParserWrap template2ParserWrap = helper.createTemplate2ParserWrap(p.toFile());
                     try {
                         Template2ParserListener listener = new Template2ParserListener(configuration, template2ParserWrap);
                         ParseTree tree = template2ParserWrap.getParser().program();
                         ParseTreeWalker walker = new ParseTreeWalker();
                         walker.walk(listener, tree);
                         javaFiles.put(listener.getClassName(), listener.getOutputJavaFile());
                     } catch (IOException e) {
                         log.error("create template parser listener exception", e);
                     }
                 });
        } catch (IOException e) {
            log.error("generate java file exception", e);
        }
    }
}
