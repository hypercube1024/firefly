package com.firefly.template2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class TestTemplate2Compiler {
    public static void main(String[] args) throws URISyntaxException, UnsupportedEncodingException {
        Configuration configuration = new Configuration();
        configuration.setTemplateHome(new File(Template2Compiler.class.getResource("/").toURI()).getAbsolutePath());
        System.out.println(configuration.getTemplateHome());

        Template2Compiler compiler = new Template2Compiler(configuration);
        Map<String, File> javaFiles = compiler.generateJavaFiles();
        System.out.println("generate java file size: " + javaFiles.size());
        compiler.compileJavaFiles(javaFiles);

        Template2Loader loader = new Template2Loader();
        loader.loadAllClass(configuration.getRootPath().toPath());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.render(configuration.getPackagePrefix() + ".TestMain", out, new HashMap<>());
        System.out.println(new String(out.toByteArray(), configuration.getOutputJavaFileCharset()));
    }
}
