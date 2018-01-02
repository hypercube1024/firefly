package com.firefly.codec.websocket.model;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.SharedBlockingCallback;

import java.io.IOException;

/**
 * Extends a {@link SharedBlockingCallback} to a WebSocket {@link WriteCallback}
 */
public class BlockingWriteCallback extends SharedBlockingCallback {
    public BlockingWriteCallback() {
    }

    public WriteBlocker acquireWriteBlocker() throws IOException {
        return new WriteBlocker(acquire());
    }

    public static class WriteBlocker implements WriteCallback, Callback, AutoCloseable {
        private final Blocker blocker;

        protected WriteBlocker(Blocker blocker) {
            this.blocker = blocker;
        }

        @Override
        public void writeFailed(Throwable x) {
            blocker.failed(x);
        }

        @Override
        public void writeSuccess() {
            blocker.succeeded();
        }

        @Override
        public void succeeded() {
            blocker.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            blocker.failed(x);
        }

        @Override
        public void close() {
            blocker.close();
        }

        public void block() throws IOException {
            blocker.block();
        }
    }
}
