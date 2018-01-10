package com.firefly.codec.websocket.stream;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.model.Extension;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.codec.websocket.model.extension.ExtensionFactory;
import com.firefly.codec.websocket.model.extension.WebSocketExtensionFactory;
import com.firefly.utils.Assert;
import com.firefly.utils.CollectionUtils;

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

    public List<ExtensionConfig> negotiate(MetaData metaData) {
        return ExtensionConfig.parseEnum(metaData.getFields().getValues(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.asString()))
                              .stream().filter(c -> factory.isAvailable(c.getName()))
                              .collect(Collectors.toList());
    }

    public List<Extension> parse(MetaData metaData) {
        Assert.notNull(nextIncomingFrames, "The next incoming frames MUST be not null");
        Assert.notNull(nextOutgoingFrames, "The next outgoing frames MUST be not null");

        List<Extension> extensions = ExtensionConfig.parseEnum(metaData.getFields().getValues(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.asString()))
                                                    .stream().filter(c -> factory.isAvailable(c.getName()))
                                                    .map(factory::newInstance)
                                                    .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(extensions)) {
            for (int i = 0; i < extensions.size(); i++) {
                int next = i + 1;
                if (next < extensions.size() - 1) {
                    extensions.get(i).setNextIncomingFrames(extensions.get(next));
                } else {
                    extensions.get(i).setNextIncomingFrames(nextIncomingFrames);
                }
            }
            incomingFrames = extensions.get(0);

            for (int i = extensions.size() - 1; i >= 0; i--) {
                int next = i - 1;
                if (next > 0) {
                    extensions.get(i).setNextOutgoingFrames(extensions.get(next));
                } else {
                    extensions.get(i).setNextOutgoingFrames(nextOutgoingFrames);
                }
            }
            outgoingFrames = extensions.get(extensions.size() - 1);
            return extensions;
        } else {
            incomingFrames = nextIncomingFrames;
            outgoingFrames = nextOutgoingFrames;
            return Collections.emptyList();
        }
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
