package com.fireflysource.net.websocket.common.extension.fragment;

import com.fireflysource.common.concurrent.AutoLock;
import com.fireflysource.common.concurrent.IteratingCallback;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.extension.AbstractExtension;
import com.fireflysource.net.websocket.common.frame.DataFrame;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.model.ExtensionConfig;
import com.fireflysource.net.websocket.common.model.OpCode;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Fragment Extension
 */
public class FragmentExtension extends AbstractExtension {
    private static LazyLogger LOG = SystemLogger.create(FragmentExtension.class);

    private final AutoLock lock = new AutoLock();
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
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        ByteBuffer payload = frame.getPayload();
        int length = payload != null ? payload.remaining() : 0;
        if (OpCode.isControlFrame(frame.getOpCode()) || maxLength <= 0 || length <= maxLength) {
            nextOutgoingFrame(frame, result);
            return;
        }

        FrameEntry entry = new FrameEntry(frame, result);
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
        lock.lock(() -> entries.offer(entry));
    }

    private FrameEntry pollEntry() {
        return lock.lock(entries::poll);
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }

    private static class FrameEntry {
        private final Frame frame;
        private final Consumer<Result<Void>> result;

        private FrameEntry(Frame frame, Consumer<Result<Void>> result) {
            this.frame = frame;
            this.result = result;
        }

        @Override
        public String toString() {
            return frame.toString();
        }
    }

    private class Flusher extends IteratingCallback {
        private FrameEntry current;
        private boolean finished = true;

        @Override
        protected Action process() {
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

            nextOutgoingFrame(fragment, this);
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
        public void accept(Result<Void> result) {
            if (!result.isSuccess()) {
                notifyCallbackFailure(current.result, result.getThrowable());
            }
            notifyCallbackSuccess(current.result);
            super.accept(result);
        }

        private void notifyCallbackSuccess(Consumer<Result<Void>> result) {
            try {
                if (result != null)
                    result.accept(Result.SUCCESS);
            } catch (Throwable x) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while notifying success", x);
            }
        }

        private void notifyCallbackFailure(Consumer<Result<Void>> result, Throwable failure) {
            try {
                if (result != null)
                    result.accept(Result.createFailedResult(failure));
            } catch (Throwable x) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while notifying failure", x);
            }
        }
    }
}
