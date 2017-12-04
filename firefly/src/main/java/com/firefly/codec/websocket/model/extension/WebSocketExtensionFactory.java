package com.firefly.codec.websocket.model.extension;

import com.firefly.codec.websocket.exception.WebSocketException;
import com.firefly.codec.websocket.model.Extension;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.utils.StringUtils;

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
            return extClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new WebSocketException("Cannot instantiate extension: " + extClass, e);
        }
    }
}
