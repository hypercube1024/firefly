package com.firefly.codec.websocket.model.extension;

import com.firefly.codec.websocket.model.Extension;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.extension.compress.DeflateFrameExtension;
import com.firefly.codec.websocket.model.extension.compress.PerMessageDeflateExtension;
import com.firefly.codec.websocket.model.extension.compress.XWebkitDeflateFrameExtension;
import com.firefly.utils.CollectionUtils;

import java.util.*;

public abstract class ExtensionFactory implements Iterable<Class<? extends Extension>> {
    private ServiceLoader<Extension> extensionLoader = ServiceLoader.load(Extension.class);
    private Map<String, Class<? extends Extension>> availableExtensions;

    public ExtensionFactory() {
        availableExtensions = new HashMap<>();
        for (Extension ext : extensionLoader) {
            if (ext != null) {
                availableExtensions.put(ext.getName(), ext.getClass());
            }
        }
        if (CollectionUtils.isEmpty(availableExtensions)) {
            availableExtensions.put(new DeflateFrameExtension().getName(), DeflateFrameExtension.class);
            availableExtensions.put(new PerMessageDeflateExtension().getName(), PerMessageDeflateExtension.class);
            availableExtensions.put(new XWebkitDeflateFrameExtension().getName(), PerMessageDeflateExtension.class);
        }
    }

    public Map<String, Class<? extends Extension>> getAvailableExtensions() {
        return availableExtensions;
    }

    public Class<? extends Extension> getExtension(String name) {
        return availableExtensions.get(name);
    }

    public Set<String> getExtensionNames() {
        return availableExtensions.keySet();
    }

    public boolean isAvailable(String name) {
        return availableExtensions.containsKey(name);
    }

    @Override
    public Iterator<Class<? extends Extension>> iterator() {
        return availableExtensions.values().iterator();
    }

    public abstract Extension newInstance(ExtensionConfig config);

    public void register(String name, Class<? extends Extension> extension) {
        availableExtensions.put(name, extension);
    }

    public void unregister(String name) {
        availableExtensions.remove(name);
    }
}
