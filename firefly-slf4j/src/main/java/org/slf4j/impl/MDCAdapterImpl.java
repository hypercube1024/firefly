package org.slf4j.impl;

import com.firefly.utils.log.Log;
import org.slf4j.spi.MDCAdapter;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class MDCAdapterImpl implements MDCAdapter {
    @Override
    public void put(String key, String val) {
        Log.mdc.put(key, val);
    }

    @Override
    public String get(String key) {
        return Log.mdc.get(key);
    }

    @Override
    public void remove(String key) {
        Log.mdc.remove(key);
    }

    @Override
    public void clear() {
        Log.mdc.clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return Log.mdc.getCopyOfContextMap();
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        Log.mdc.setContextMap(contextMap);
    }
}
