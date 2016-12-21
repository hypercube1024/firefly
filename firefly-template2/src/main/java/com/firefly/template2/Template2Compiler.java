package com.firefly.template2;

import com.firefly.template2.generator.Template2ParserListener;
import com.firefly.template2.parser.helper.Template2ParserHelper;
import com.firefly.template2.parser.helper.Template2ParserWrap;
import com.firefly.template2.utils.JavaCompilerUtils;
import com.firefly.utils.lang.Pair;
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

    public Map<String, File> generateJavaFiles() {
        Map<String, File> javaFiles = new HashMap<>();
        Path path = Paths.get(configuration.getTemplateHome());
        try {
            Files.walk(path)
                 .filter(p -> p.getFileName().toString().endsWith("." + configuration.getSuffix()))
                 .forEach(p -> {
                     if (log.isDebugEnabled()) {
                         log.debug("find template file -> {}", p.toFile().getName());
                     }
                     Pair<String, File> pair = generateJavaFile(p);
                     if (pair != null) {
                         javaFiles.put(pair.first, pair.second);
                     }
                 });
        } catch (IOException e) {
            log.error("generate java file exception", e);
        }
        return javaFiles;
    }

    public Pair<String, File> generateJavaFile(Path p) {
        Template2ParserHelper helper = new Template2ParserHelper(configuration);
        Template2ParserWrap template2ParserWrap = helper.createTemplate2ParserWrap(p.toFile());
        try {
            Template2ParserListener listener = new Template2ParserListener(configuration, template2ParserWrap);
            if (listener.isOutput()) {
                ParseTree tree = template2ParserWrap.getParser().program();
                ParseTreeWalker walker = new ParseTreeWalker();
                walker.walk(listener, tree);
                Pair<String, File> javaFile = new Pair<>();
                javaFile.first = listener.getClassName();
                javaFile.second = listener.getOutputJavaFile();
                return javaFile;
            }
        } catch (IOException e) {
            log.error("create template parser listener exception", e);
        }
        return null;
    }

    public int compileJavaFiles(Map<String, File> javaFiles) {
        if (!javaFiles.isEmpty()) {
            int ret = JavaCompilerUtils.compile(configuration.getRootPath().getAbsolutePath(),
                    getClassPath(),
                    configuration.getOutputJavaFileCharset(),
                    javaFiles.entrySet()
                             .stream()
                             .map(Map.Entry::getValue)
                             .map(File::getAbsolutePath)
                             .collect(Collectors.toList()));
            if (ret != 0) {
                log.error("java file compiling exception");
            }
            return ret;
        } else {
            return 0;
        }
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
