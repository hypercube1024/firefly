package com.firefly.template2;

import com.firefly.utils.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class TestTemplate2Compiler {
    public static void main(String[] args) throws URISyntaxException {
        Configuration configuration = new Configuration();
        configuration.setTemplateHome(new File(Template2Compiler.class.getResource("/").toURI()).getAbsolutePath());
        System.out.println(configuration.getTemplateHome());

        Template2Compiler compiler = new Template2Compiler(configuration);
        Map<String, File> toCompiledJavaFiles =  compiler.generateJavaFiles();
        compiler.getJavaFiles().entrySet().forEach(entry -> {
            String className = entry.getKey();
            File file = entry.getValue();

            try {
                System.out.println("--------------------" + className + "----------------------------");
                System.out.println(FileUtils.readFileToString(file, configuration.getOutputJavaFileCharset()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.out.println(toCompiledJavaFiles.size());
        compiler.compileJavaFiles(toCompiledJavaFiles);
    }
}
