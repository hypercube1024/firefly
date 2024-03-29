package com.fireflysource.net.websocket.common.extension;


import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.net.websocket.common.extension.compress.DeflateFrameExtension;
import com.fireflysource.net.websocket.common.extension.compress.PerMessageDeflateExtension;
import com.fireflysource.net.websocket.common.extension.compress.XWebkitDeflateFrameExtension;
import com.fireflysource.net.websocket.common.extension.fragment.FragmentExtension;
import com.fireflysource.net.websocket.common.extension.identity.IdentityExtension;
import com.fireflysource.net.websocket.common.model.Extension;
import com.fireflysource.net.websocket.common.model.ExtensionConfig;

import java.util.*;

@SuppressWarnings("unused")
public abstract class ExtensionFactory implements Iterable<Class<? extends Extension>> {
    private static final ServiceLoader<Extension> extensionLoader = ServiceLoader.load(Extension.class);
    private final Map<String, Class<? extends Extension>> availableExtensions;

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
