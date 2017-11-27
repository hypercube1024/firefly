package com.firefly.codec.websocket.model.extension.identity;


import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.WriteCallback;
import com.firefly.codec.websocket.model.extension.AbstractExtension;
import com.firefly.utils.lang.QuotedStringTokenizer;

public class IdentityExtension extends AbstractExtension {
    private String id;

    public IdentityExtension() {
        start();
    }

    public String getParam(String key) {
        return getConfig().getParameter(key, "?");
    }

    @Override
    public String getName() {
        return "identity";
    }

    @Override
    public void incomingError(Throwable e) {
        // pass through
        nextIncomingError(e);
    }

    @Override
    public void incomingFrame(Frame frame) {
        // pass through
        nextIncomingFrame(frame);
    }

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {
        // pass through
        nextOutgoingFrame(frame, callback, batchMode);
    }

    @Override
    public void setConfig(ExtensionConfig config) {
        super.setConfig(config);
        StringBuilder s = new StringBuilder();
        s.append(config.getName());
        s.append("@").append(Integer.toHexString(hashCode()));
        s.append("[");
        boolean delim = false;
        for (String param : config.getParameterKeys()) {
            if (delim) {
                s.append(';');
            }
            s.append(param).append('=').append(QuotedStringTokenizer.quoteIfNeeded(config.getParameter(param, ""), ";="));
            delim = true;
        }
        s.append("]");
        id = s.toString();
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }
}
