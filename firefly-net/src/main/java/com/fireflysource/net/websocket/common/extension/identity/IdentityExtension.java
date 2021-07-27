package com.fireflysource.net.websocket.common.extension.identity;


import com.fireflysource.common.string.QuotedStringTokenizer;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.common.extension.AbstractExtension;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.model.ExtensionConfig;

import java.util.function.Consumer;

public class IdentityExtension extends AbstractExtension {

    private String id;

    public String getParam(String key) {
        return getConfig().getParameter(key, "?");
    }

    @Override
    public String getName() {
        return "identity";
    }

    @Override
    public void incomingFrame(Frame frame) {
        // pass through
        nextIncomingFrame(frame);
    }

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        // pass through
        nextOutgoingFrame(frame, result);
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

}
