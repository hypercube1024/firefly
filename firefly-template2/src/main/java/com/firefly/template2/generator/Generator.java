package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class Generator {

    private Map<Class<?>, JavaGenerator> map = new HashMap<>();

    public Generator(Configuration configuration) {
        map.put(Template2Parser.ProgramContext.class, new ProgramGenerator(configuration));
        map.put(Template2Parser.ExtendTemplateContext.class, new ExtendTemplateGenerator(configuration));
        map.put(Template2Parser.MainFunctionContext.class, new MainFunctionGenerator(configuration));
        map.put(Template2Parser.OutputContext.class, new OutputGenerator(configuration));
    }

    public <T extends ParseTree, R> R getGenerator(Class<T> parseTree) {
        return (R)map.get(parseTree);
    }
}
