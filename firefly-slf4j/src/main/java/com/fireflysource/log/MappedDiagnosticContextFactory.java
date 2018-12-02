package com.fireflysource.log;

import com.fireflysource.log.internal.utils.ServiceUtils;

/**
 * @author Pengtao Qiu
 */
public class MappedDiagnosticContextFactory {

    private static MappedDiagnosticContextFactory ourInstance = new MappedDiagnosticContextFactory();

    public static MappedDiagnosticContextFactory getInstance() {
        return ourInstance;
    }

    private MappedDiagnosticContext mappedDiagnosticContext;

    private MappedDiagnosticContextFactory() {
        mappedDiagnosticContext = ServiceUtils.loadService(MappedDiagnosticContext.class, new ThreadLocalMappedDiagnosticContext());
    }

    public MappedDiagnosticContext getMappedDiagnosticContext() {
        return mappedDiagnosticContext;
    }
}
