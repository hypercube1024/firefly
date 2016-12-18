package com.firefly.template2.generator;

import com.firefly.template2.parser.Template2Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
abstract public class Generator {

    private static final Map<Class<?>, JavaGenerator> map = new HashMap<>();

    static {
        map.put(Template2Parser.ProgramContext.class, new ProgramGenerator());
    }

    public static <T> JavaGenerator getGenerator(Class<T> parseTree) {
        return map.get(parseTree);
    }
}
