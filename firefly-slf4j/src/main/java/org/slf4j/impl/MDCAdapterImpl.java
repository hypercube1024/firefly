package org.slf4j.impl;

import com.firefly.utils.log.MappedDiagnosticContext;
import com.firefly.utils.log.MappedDiagnosticContextFactory;
import org.slf4j.spi.MDCAdapter;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class MDCAdapterImpl implements MDCAdapter {

    private MappedDiagnosticContext mdc;

    public MDCAdapterImpl() {
        mdc = MappedDiagnosticContextFactory.getInstance().getMappedDiagnosticContext();
    }

    @Override
    public void put(String key, String val) {
        mdc.put(key, val);
    }

    @Override
    public String get(String key) {
        return mdc.get(key);
    }

    @Override
    public void remove(String key) {
        mdc.remove(key);
    }

    @Override
    public void clear() {
        mdc.clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return mdc.getCopyOfContextMap();
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        mdc.setContextMap(contextMap);
    }
}
