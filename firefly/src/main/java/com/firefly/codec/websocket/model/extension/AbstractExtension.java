package com.firefly.codec.websocket.model.extension;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.Extension;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractExtension extends AbstractLifeCycle implements Extension {
    private static Logger log = LoggerFactory.getLogger("firefly-system");
    private WebSocketPolicy policy;
    private ExtensionConfig config;
    private OutgoingFrames nextOutgoing;
    private IncomingFrames nextIncoming;

    public AbstractExtension() {
    }

    public void dump(Appendable out, String indent) throws IOException {
        // incoming
        dumpWithHeading(out, indent, "incoming", this.nextIncoming);
        dumpWithHeading(out, indent, "outgoing", this.nextOutgoing);
    }

    protected void dumpWithHeading(Appendable out, String indent, String heading, Object bean) throws IOException {
        out.append(indent).append(" +- ");
        out.append(heading).append(" : ");
        out.append(bean.toString());
    }

    @Override
    public ExtensionConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    public IncomingFrames getNextIncoming() {
        return nextIncoming;
    }

    public OutgoingFrames getNextOutgoing() {
        return nextOutgoing;
    }

    public WebSocketPolicy getPolicy() {
        return policy;
    }

    @Override
    public void incomingError(Throwable e) {
        nextIncomingError(e);
    }

    /**
     * Used to indicate that the extension makes use of the RSV1 bit of the base websocket framing.
     * <p>
     * This is used to adjust validation during parsing, as well as a checkpoint against 2 or more extensions all simultaneously claiming ownership of RSV1.
     *
     * @return true if extension uses RSV1 for its own purposes.
     */
    @Override
    public boolean isRsv1User() {
        return false;
    }

    /**
     * Used to indicate that the extension makes use of the RSV2 bit of the base websocket framing.
     * <p>
     * This is used to adjust validation during parsing, as well as a checkpoint against 2 or more extensions all simultaneously claiming ownership of RSV2.
     *
     * @return true if extension uses RSV2 for its own purposes.
     */
    @Override
    public boolean isRsv2User() {
        return false;
    }

    /**
     * Used to indicate that the extension makes use of the RSV3 bit of the base websocket framing.
     * <p>
     * This is used to adjust validation during parsing, as well as a checkpoint against 2 or more extensions all simultaneously claiming ownership of RSV3.
     *
     * @return true if extension uses RSV3 for its own purposes.
     */
    @Override
    public boolean isRsv3User() {
        return false;
    }

    protected void nextIncomingError(Throwable e) {
        this.nextIncoming.incomingError(e);
    }

    protected void nextIncomingFrame(Frame frame) {
        log.debug("nextIncomingFrame({})", frame);
        this.nextIncoming.incomingFrame(frame);
    }

    protected void nextOutgoingFrame(Frame frame, Callback callback) {
        log.debug("nextOutgoingFrame({})", frame);
        this.nextOutgoing.outgoingFrame(frame, callback);
    }

    public void setConfig(ExtensionConfig config) {
        this.config = config;
    }

    @Override
    public void setNextIncomingFrames(IncomingFrames nextIncoming) {
        this.nextIncoming = nextIncoming;
    }

    @Override
    public void setNextOutgoingFrames(OutgoingFrames nextOutgoing) {
        this.nextOutgoing = nextOutgoing;
    }

    public void setPolicy(WebSocketPolicy policy) {
        this.policy = policy;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), config.getParameterizedName());
    }
}
