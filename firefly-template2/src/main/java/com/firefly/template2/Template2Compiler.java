package com.firefly.template2;

import com.firefly.template2.generator.Template2ParserListener;
import com.firefly.template2.utils.CompileUtils;
import com.firefly.template2.utils.Template2ParserHelper;
import com.firefly.template2.utils.Template2ParserWrap;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Template2Compiler generateJavaFiles() {
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
        return this;
    }

    public Template2Compiler compileJavaFiles() {
        if (!javaFiles.isEmpty()) {
            CompileUtils.compile(configuration.getRootPath().getAbsolutePath(), getClassPath(),
                    configuration.getOutputJavaFileCharset(),
                    javaFiles.entrySet()
                             .stream()
                             .map(Map.Entry::getValue)
                             .map(File::getAbsolutePath)
                             .collect(Collectors.toList())
            );
        }
        return this;
    }

    public String getClassPath() {
        URL url = this.getClass().getResource("");
        if ("jar".equals(url.getProtocol())) {
            String f = url.getPath();
            try {
                return new File(new URL(f.substring(0,
                        f.indexOf("!/com/firefly"))).toURI()).getAbsolutePath();
            } catch (Throwable t) {
                log.error("get class path exception", t);
                return null;
            }
        } else {
            return null;
        }
    }
}
