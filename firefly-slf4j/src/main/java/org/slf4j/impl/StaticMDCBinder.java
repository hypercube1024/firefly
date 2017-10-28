package org.slf4j.impl;

import com.firefly.utils.ServiceUtils;
import org.slf4j.spi.MDCAdapter;

/**
 * @author Pengtao Qiu
 */
public class StaticMDCBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();
    private MDCAdapter mdca;

    private StaticMDCBinder() {
        mdca = ServiceUtils.loadService(org.slf4j.spi.MDCAdapter.class, new MDCAdapterImpl());
    }

    public MDCAdapter getMDCA() {
        return mdca;
    }

    public String getMDCAdapterClassStr() {
        return mdca.getClass().getName();
    }
}
