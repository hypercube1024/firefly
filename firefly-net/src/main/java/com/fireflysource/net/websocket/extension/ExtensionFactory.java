package com.fireflysource.net.websocket.extension;


import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.net.websocket.extension.compress.DeflateFrameExtension;
import com.fireflysource.net.websocket.extension.compress.PerMessageDeflateExtension;
import com.fireflysource.net.websocket.extension.compress.XWebkitDeflateFrameExtension;
import com.fireflysource.net.websocket.extension.fragment.FragmentExtension;
import com.fireflysource.net.websocket.extension.identity.IdentityExtension;
import com.fireflysource.net.websocket.model.Extension;
import com.fireflysource.net.websocket.model.ExtensionConfig;

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
            availableExtensions.put(new XWebkitDeflateFrameExtension().getName(), XWebkitDeflateFrameExtension.class);
            availableExtensions.put(new IdentityExtension().getName(), IdentityExtension.class);
            availableExtensions.put(new FragmentExtension().getName(), FragmentExtension.class);
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
