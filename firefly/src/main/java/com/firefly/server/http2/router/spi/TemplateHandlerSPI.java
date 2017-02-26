package com.firefly.server.http2.router.spi;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface TemplateHandlerSPI {

    void renderTemplate(String resourceName, Object scope);

    void renderTemplate(String resourceName, Object[] scopes);

    void renderTemplate(String resourceName, List<Object> scopes);

}
