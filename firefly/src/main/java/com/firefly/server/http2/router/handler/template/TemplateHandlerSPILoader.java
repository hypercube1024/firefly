package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.spi.TemplateHandlerSPI;

import java.util.ServiceLoader;

/**
 * @author Pengtao Qiu
 */
public class TemplateHandlerSPILoader {
    private static TemplateHandlerSPILoader ourInstance = new TemplateHandlerSPILoader();

    public static TemplateHandlerSPILoader getInstance() {
        return ourInstance;
    }

    private TemplateHandlerSPI templateHandlerSPI;

    private TemplateHandlerSPILoader() {
        ServiceLoader<TemplateHandlerSPI> serviceLoader = ServiceLoader.load(TemplateHandlerSPI.class);
        for(TemplateHandlerSPI s : serviceLoader) {
            templateHandlerSPI = s;
        }
        if (templateHandlerSPI == null) {
            templateHandlerSPI = new MustacheTemplateHandlerSPIImpl();
        }
    }

    public TemplateHandlerSPI getTemplateHandlerSPI() {
        return templateHandlerSPI;
    }
}
