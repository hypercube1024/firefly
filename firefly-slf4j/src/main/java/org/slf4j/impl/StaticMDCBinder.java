package org.slf4j.impl;

import org.slf4j.spi.MDCAdapter;

/**
 * @author Pengtao Qiu
 */
public class StaticMDCBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private StaticMDCBinder() {
    }

    public MDCAdapter getMDCA() {
        return new MDCAdapterImpl();
    }

    public String getMDCAdapterClassStr() {
        return MDCAdapterImpl.class.getName();
    }
}
