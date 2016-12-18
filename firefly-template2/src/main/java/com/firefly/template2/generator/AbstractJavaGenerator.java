package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractJavaGenerator<T> implements JavaGenerator<T> {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected Configuration configuration;

    public AbstractJavaGenerator(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void exit(T node, Writer writer, Object... args) {
        try {
            if (args.length > 0 && args[0] instanceof Integer) {
                Integer levelCount = (Integer) args[0];
                for (int i = 0; i < levelCount; i++) {
                    writer.append("    ");
                }
            }
            writer.append('}').append(configuration.getLineSeparator());
        } catch (IOException e) {
            log.error("exit program generator exception", e);
        }
    }

}
