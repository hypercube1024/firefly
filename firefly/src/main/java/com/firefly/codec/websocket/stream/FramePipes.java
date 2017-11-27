package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.codec.websocket.model.WriteCallback;

public class FramePipes {
    private static class In2Out implements IncomingFrames {
        private OutgoingFrames outgoing;

        public In2Out(OutgoingFrames outgoing) {
            this.outgoing = outgoing;
        }

        @Override
        public void incomingError(Throwable t) {
            /* cannot send exception on */
        }

        @Override
        public void incomingFrame(Frame frame) {
            this.outgoing.outgoingFrame(frame, null, BatchMode.OFF);
        }
    }

    private static class Out2In implements OutgoingFrames {
        private IncomingFrames incoming;

        public Out2In(IncomingFrames incoming) {
            this.incoming = incoming;
        }

        @Override
        public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {
            try {
                this.incoming.incomingFrame(frame);
                callback.writeSuccess();
            } catch (Throwable t) {
                callback.writeFailed(t);
            }
        }
    }

    public static OutgoingFrames to(final IncomingFrames incoming) {
        return new Out2In(incoming);
    }

    public static IncomingFrames to(final OutgoingFrames outgoing) {
        return new In2Out(outgoing);
    }
}
