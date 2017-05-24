package com.firefly.codec.http2.model;

import java.util.EventListener;

/**
 * A {@link ContentProvider} that notifies listeners that content is available.
 */
public interface AsyncContentProvider extends ContentProvider {
    /**
     * @param listener the listener to be notified of content availability
     */
    void setListener(Listener listener);

    /**
     * A listener that is notified of content availability
     */
    interface Listener extends EventListener {
        /**
         * Callback method invoked when content is available
         */
        void onContent();
    }
}
