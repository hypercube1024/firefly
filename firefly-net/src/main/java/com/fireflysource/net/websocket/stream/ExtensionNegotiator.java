package com.fireflysource.net.websocket.stream;


import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.common.object.Assert;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpHeader;
import com.fireflysource.net.websocket.extension.AbstractExtension;
import com.fireflysource.net.websocket.extension.ExtensionFactory;
import com.fireflysource.net.websocket.extension.WebSocketExtensionFactory;
import com.fireflysource.net.websocket.model.Extension;
import com.fireflysource.net.websocket.model.ExtensionConfig;
import com.fireflysource.net.websocket.model.IncomingFrames;
import com.fireflysource.net.websocket.model.OutgoingFrames;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.fireflysource.net.websocket.model.ExtensionConfig.parseEnum;


/**
 * @author Pengtao Qiu
 */
public class ExtensionNegotiator {

    private ExtensionFactory factory;
    private IncomingFrames nextIncomingFrames;
    private OutgoingFrames nextOutgoingFrames;
    private IncomingFrames incomingFrames;
    private OutgoingFrames outgoingFrames;

    public ExtensionNegotiator() {
        this(new WebSocketExtensionFactory());
    }

    public ExtensionNegotiator(ExtensionFactory factory) {
        this.factory = factory;
    }

    public ExtensionFactory getFactory() {
        return factory;
    }

    public void setFactory(ExtensionFactory factory) {
        this.factory = factory;
    }

    public List<ExtensionConfig> createExtensionConfigs(HttpFields fields) {
        return parseEnum(fields.getValues(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.getValue()))
                .stream()
                .filter(c -> factory.isAvailable(c.getName()))
                .collect(Collectors.toList());
    }

    public List<Extension> createExtensionChain(HttpFields fields) {
        Assert.notNull(nextIncomingFrames, "The next incoming frames MUST be not null");
        Assert.notNull(nextOutgoingFrames, "The next outgoing frames MUST be not null");

        List<Extension> extensions = createExtensions(fields);
        if (!CollectionUtils.isEmpty(extensions)) {
            for (int incoming = 0, outgoing = extensions.size() - 1; incoming < extensions.size() && outgoing >= 0; incoming++, outgoing--) {
                int nextIncoming = incoming + 1;
                if (nextIncoming < extensions.size() - 1) {
                    extensions.get(incoming).setNextIncomingFrames(extensions.get(nextIncoming));
                } else {
                    extensions.get(incoming).setNextIncomingFrames(nextIncomingFrames);
                }

                int nextOutgoing = outgoing - 1;
                if (nextOutgoing > 0) {
                    extensions.get(outgoing).setNextOutgoingFrames(extensions.get(nextOutgoing));
                } else {
                    extensions.get(outgoing).setNextOutgoingFrames(nextOutgoingFrames);
                }
            }
            incomingFrames = extensions.get(0);
            outgoingFrames = extensions.get(extensions.size() - 1);
            return extensions;
        } else {
            incomingFrames = nextIncomingFrames;
            outgoingFrames = nextOutgoingFrames;
            return Collections.emptyList();
        }
    }

    private List<Extension> createExtensions(HttpFields fields) {
        return parseEnum(fields.getValues(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.getValue()))
                .stream().filter(c -> factory.isAvailable(c.getName()))
                .map(c -> {
                    Extension e = factory.newInstance(c);
                    if (e instanceof AbstractExtension) {
                        AbstractExtension abstractExtension = (AbstractExtension) e;
                        abstractExtension.setConfig(c);
                    }
                    return e;
                })
                .collect(Collectors.toList());
    }

    public IncomingFrames getNextIncomingFrames() {
        return nextIncomingFrames;
    }

    public void setNextIncomingFrames(IncomingFrames nextIncomingFrames) {
        this.nextIncomingFrames = nextIncomingFrames;
    }

    public OutgoingFrames getNextOutgoingFrames() {
        return nextOutgoingFrames;
    }

    public void setNextOutgoingFrames(OutgoingFrames nextOutgoingFrames) {
        this.nextOutgoingFrames = nextOutgoingFrames;
    }

    public IncomingFrames getIncomingFrames() {
        return incomingFrames;
    }

    public OutgoingFrames getOutgoingFrames() {
        return outgoingFrames;
    }

}
