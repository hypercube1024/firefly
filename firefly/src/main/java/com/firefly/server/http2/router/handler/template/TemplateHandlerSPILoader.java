package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * @author Pengtao Qiu
 */
public class TemplateHandlerSPILoader {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private static TemplateHandlerSPILoader ourInstance = new TemplateHandlerSPILoader();

    public static TemplateHandlerSPILoader getInstance() {
        return ourInstance;
    }

    private TemplateHandlerSPI templateHandlerSPI;

    private TemplateHandlerSPILoader() {
        ServiceLoader<TemplateHandlerSPI> serviceLoader = ServiceLoader.load(TemplateHandlerSPI.class);
        for(TemplateHandlerSPI s : serviceLoader) {
            templateHandlerSPI = s;
            log.info("load TemplateHandlerSPI, implement class -> {}", s.getClass().getName());
        }
        if (templateHandlerSPI == null) {
            templateHandlerSPI = new MustacheTemplateHandlerSPIImpl();
        }
        log.info("load TemplateHandlerSPI, selected -> {}", templateHandlerSPI.getClass().getName());
    }

    public TemplateHandlerSPI getTemplateHandlerSPI() {
        return templateHandlerSPI;
    }
}
