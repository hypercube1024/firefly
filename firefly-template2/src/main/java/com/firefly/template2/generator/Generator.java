package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class Generator {

    private Map<Class<?>, JavaGenerator> map = new HashMap<>();

    public Generator(Configuration configuration) {
        map.put(Template2Parser.ProgramContext.class, new ProgramGenerator(configuration));
    }

    public <T> JavaGenerator getGenerator(Class<T> parseTree) {
        return map.get(parseTree);
    }
}
