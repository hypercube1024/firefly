package com.fireflysource.net.websocket.common.stream;


import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.common.object.Assert;
import com.fireflysource.net.http.common.model.HttpFields;
import com.fireflysource.net.http.common.model.HttpHeader;
import com.fireflysource.net.websocket.common.decoder.Parser;
import com.fireflysource.net.websocket.common.encoder.Generator;
import com.fireflysource.net.websocket.common.extension.AbstractExtension;
import com.fireflysource.net.websocket.common.extension.ExtensionFactory;
import com.fireflysource.net.websocket.common.extension.WebSocketExtensionFactory;
import com.fireflysource.net.websocket.common.model.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


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
        return ExtensionConfig
                .parseEnum(fields.getValues(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.getValue()))
                .stream()
                .filter(c -> factory.isAvailable(c.getName()))
                .collect(Collectors.toList());
    }

    public void configureExtensions(HttpFields fields, Parser parser, Generator generator, WebSocketPolicy policy) {
        Assert.notNull(nextIncomingFrames, "The next incoming frames MUST be not null");
        Assert.notNull(nextOutgoingFrames, "The next outgoing frames MUST be not null");

        List<ExtensionConfig> extensionConfigs = createExtensionConfigs(fields);
        if (CollectionUtils.isEmpty(extensionConfigs)) {
            incomingFrames = nextIncomingFrames;
            outgoingFrames = nextOutgoingFrames;
        } else {
            List<Extension> incomingExtensions = createExtensions(extensionConfigs, policy);
            List<Extension> outgoingExtensions = createExtensions(extensionConfigs, policy);

            Collections.reverse(incomingExtensions);

            parser.configureFromExtensions(incomingExtensions);
            generator.configureFromExtensions(outgoingExtensions);

            int lastIncoming = incomingExtensions.size() - 1;
            for (int i = 0; i < incomingExtensions.size(); i++) {
                int next = i + 1;
                Extension extension = incomingExtensions.get(i);
                if (next <= lastIncoming) {
                    extension.setNextIncomingFrames(incomingExtensions.get(next));
                } else {
                    extension.setNextIncomingFrames(nextIncomingFrames);
                }
            }

            int lastOutgoing = outgoingExtensions.size() - 1;
            for (int i = 0; i < outgoingExtensions.size(); i++) {
                int next = i + 1;
                Extension extension = outgoingExtensions.get(i);
                if (next <= lastOutgoing) {
                    extension.setNextOutgoingFrames(outgoingExtensions.get(next));
                } else {
                    extension.setNextOutgoingFrames(nextOutgoingFrames);
                }
            }

            incomingFrames = incomingExtensions.get(0);
            outgoingFrames = outgoingExtensions.get(0);
        }
    }

    private List<Extension> createExtensions(List<ExtensionConfig> extensionConfigs, WebSocketPolicy policy) {
        return extensionConfigs
                .stream()
                .map(c -> {
                    Extension e = factory.newInstance(c);
                    if (e instanceof AbstractExtension) {
                        AbstractExtension abstractExtension = (AbstractExtension) e;
                        abstractExtension.setConfig(c);
                        abstractExtension.setPolicy(policy);
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
