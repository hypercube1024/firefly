package com.fireflysource.net.websocket.common.extension;


import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.exception.WebSocketException;
import com.fireflysource.net.websocket.common.model.Extension;
import com.fireflysource.net.websocket.common.model.ExtensionConfig;

public class WebSocketExtensionFactory extends ExtensionFactory {

    @Override
    public Extension newInstance(ExtensionConfig config) {
        if (config == null) {
            return null;
        }

        String name = config.getName();
        if (!StringUtils.hasText(name)) {
            return null;
        }

        Class<? extends Extension> extClass = getExtension(name);
        if (extClass == null) {
            return null;
        }

        try {
            return extClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new WebSocketException("Cannot instantiate extension: " + extClass, e);
        }
    }
}
