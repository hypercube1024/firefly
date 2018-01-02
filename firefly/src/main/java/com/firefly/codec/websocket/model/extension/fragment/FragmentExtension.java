package com.firefly.codec.websocket.model.extension.fragment;

import com.firefly.codec.websocket.frame.DataFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.WriteCallback;
import com.firefly.codec.websocket.model.extension.AbstractExtension;
import com.firefly.utils.concurrent.IteratingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Fragment Extension
 */
public class FragmentExtension extends AbstractExtension {
    private static Logger LOG = LoggerFactory.getLogger("firefly-system");

    private final Queue<FrameEntry> entries = new ArrayDeque<>();
    private final IteratingCallback flusher = new Flusher();
    private int maxLength;

    public FragmentExtension() {
        start();
    }

    @Override
    public String getName() {
        return "fragment";
    }

    @Override
    public void incomingFrame(Frame frame) {
        nextIncomingFrame(frame);
    }

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {
        ByteBuffer payload = frame.getPayload();
        int length = payload != null ? payload.remaining() : 0;
        if (OpCode.isControlFrame(frame.getOpCode()) || maxLength <= 0 || length <= maxLength) {
            nextOutgoingFrame(frame, callback, batchMode);
            return;
        }

        FrameEntry entry = new FrameEntry(frame, callback, batchMode);
        if (LOG.isDebugEnabled())
            LOG.debug("Queuing {}", entry);
        offerEntry(entry);
        flusher.iterate();
    }

    @Override
    public void setConfig(ExtensionConfig config) {
        super.setConfig(config);
        maxLength = config.getParameter("maxLength", -1);
    }

    private void offerEntry(FrameEntry entry) {
        synchronized (this) {
            entries.offer(entry);
        }
    }

    private FrameEntry pollEntry() {
        synchronized (this) {
            return entries.poll();
        }
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }

    private static class FrameEntry {
        private final Frame frame;
        private final WriteCallback callback;
        private final BatchMode batchMode;

        private FrameEntry(Frame frame, WriteCallback callback, BatchMode batchMode) {
            this.frame = frame;
            this.callback = callback;
            this.batchMode = batchMode;
        }

        @Override
        public String toString() {
            return frame.toString();
        }
    }

    private class Flusher extends IteratingCallback implements WriteCallback {
        private FrameEntry current;
        private boolean finished = true;

        @Override
        protected Action process() throws Exception {
            if (finished) {
                current = pollEntry();
                LOG.debug("Processing {}", current);
                if (current == null)
                    return Action.IDLE;
                fragment(current, true);
            } else {
                fragment(current, false);
            }
            return Action.SCHEDULED;
        }

        private void fragment(FrameEntry entry, boolean first) {
            Frame frame = entry.frame;
            ByteBuffer payload = frame.getPayload();
            int remaining = payload.remaining();
            int length = Math.min(remaining, maxLength);
            finished = length == remaining;

            boolean continuation = frame.getType().isContinuation() || !first;
            DataFrame fragment = new DataFrame(frame, continuation);
            boolean fin = frame.isFin() && finished;
            fragment.setFin(fin);

            int limit = payload.limit();
            int newLimit = payload.position() + length;
            payload.limit(newLimit);
            ByteBuffer payloadFragment = payload.slice();
            payload.limit(limit);
            fragment.setPayload(payloadFragment);
            if (LOG.isDebugEnabled())
                LOG.debug("Fragmented {}->{}", frame, fragment);
            payload.position(newLimit);

            nextOutgoingFrame(fragment, this, entry.batchMode);
        }

        @Override
        protected void onCompleteSuccess() {
            // This IteratingCallback never completes.
        }

        @Override
        protected void onCompleteFailure(Throwable x) {
            // This IteratingCallback never fails.
            // The callback are those provided by WriteCallback (implemented
            // below) and even in case of writeFailed() we call succeeded().
        }

        @Override
        public void writeSuccess() {
            // Notify first then call succeeded(), otherwise
            // write callbacks may be invoked out of order.
            notifyCallbackSuccess(current.callback);
            succeeded();
        }

        @Override
        public void writeFailed(Throwable x) {
            // Notify first, the call succeeded() to drain the queue.
            // We don't want to call failed(x) because that will put
            // this flusher into a final state that cannot be exited,
            // and the failure of a frame may not mean that the whole
            // connection is now invalid.
            notifyCallbackFailure(current.callback, x);
            succeeded();
        }

        private void notifyCallbackSuccess(WriteCallback callback) {
            try {
                if (callback != null)
                    callback.writeSuccess();
            } catch (Throwable x) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while notifying success of callback " + callback, x);
            }
        }

        private void notifyCallbackFailure(WriteCallback callback, Throwable failure) {
            try {
                if (callback != null)
                    callback.writeFailed(failure);
            } catch (Throwable x) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while notifying failure of callback " + callback, x);
            }
        }
    }
}
