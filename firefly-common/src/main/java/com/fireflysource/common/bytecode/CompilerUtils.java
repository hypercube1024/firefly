package com.fireflysource.common.bytecode;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompilerUtils {

    public static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final Map<String, JavaFileObject> outputJavaFile = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static ClassLoader classLoader = new CompilerClassLoader(CompilerUtils.class.getClassLoader());

    public static Class<?> compileSource(String completeClassName, String source) throws IOException {
        boolean result;
        try (JavaFileManager fileManager = getStringSourceJavaFileManager(compiler,
                null, null, StandardCharsets.UTF_8)) {
            CompilationTask task = compiler.getTask(null, fileManager,
                    null, null, null,
                    Collections.singletonList(new JavaSourceFromString(completeClassName, source)));
            result = task.call();
        }

        if (!result)
            return null;

        return getClassByName(completeClassName);
    }

    public static Class<?> getClassByName(String name) {
        return classCache.computeIfAbsent(name, CompilerUtils::getClass);
    }

    private static Class<?> getClass(String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JavaFileManager getStringSourceJavaFileManager(JavaCompiler compiler, DiagnosticListener<? super JavaFileObject> diagnosticListener, Locale locale, Charset charset) {

        return new ForwardingJavaFileManager<StandardJavaFileManager>(compiler.getStandardFileManager(diagnosticListener, locale, charset)) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
                JavaFileObject jfo = new ByteJavaObject(className, kind);
                outputJavaFile.put(className, jfo);
                return jfo;
            }
        };
    }

    private static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        /**
         * The source code of this "file".
         */
        final String code;

        /**
         * Constructs a new JavaSourceFromString.
         *
         * @param name the name of the compilation unit represented by this file
         *             object
         * @param code the source code for the compilation unit represented by
         *             this file object
         */
        public JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class ByteJavaObject extends SimpleJavaFileObject {

        private ByteArrayOutputStream byteArrayOutputStream;

        public ByteJavaObject(String name, Kind kind) {
            super(toURI(name), kind);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IllegalStateException, UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream openInputStream() throws IllegalStateException, UnsupportedOperationException {
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        @Override
        public OutputStream openOutputStream() throws IllegalStateException, UnsupportedOperationException {
            return byteArrayOutputStream = new ByteArrayOutputStream();
        }
    }

    public static class CompilerClassLoader extends ClassLoader {

        public CompilerClassLoader(ClassLoader classLoader) {
            super(classLoader);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            JavaFileObject jfo = outputJavaFile.get(name);
            if (jfo != null) {
                byte[] bytes = ((ByteJavaObject) jfo).byteArrayOutputStream.toByteArray();
                outputJavaFile.remove(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }

}
