package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import com.firefly.utils.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        templateHandlerSPI = ServiceUtils.loadService(TemplateHandlerSPI.class, new MustacheTemplateHandlerSPIImpl());
        log.info("load TemplateHandlerSPI, selected -> {}", templateHandlerSPI.getClass().getName());
    }

    public TemplateHandlerSPI getTemplateHandlerSPI() {
        return templateHandlerSPI;
    }
}
