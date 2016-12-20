package com.firefly.template2.utils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.LinkedList;
import java.util.List;

public class JavaCompilerUtils {

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
        return compiler.run(null, null, null, params.toArray(new String[0]));
    }

}
