package com.firefly.template2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

public class JavaCompilerUtils {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    public static int compile(String path, String classPath, String encoding, List<String> files) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> params = new LinkedList<>();
        params.add("-encoding");
        params.add(encoding);
        params.add("-sourcepath");
        params.add(path);
        if (classPath != null) {
            params.add("-classpath");
            params.add(classPath);
        }
        params.addAll(files);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int ret = compiler.run(null, null, err, params.toArray(new String[0]));
        if (err.size() > 0) {
            log.error(new String(err.toByteArray()));
        }
        return ret;
    }

}
