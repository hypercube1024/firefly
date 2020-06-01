package com.fireflysource.net.websocket.common.extension.compress;

import com.fireflysource.common.concurrent.IteratingCallback;
import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.extension.AbstractExtension;
import com.fireflysource.net.websocket.common.frame.DataFrame;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.frame.TextFrame;
import com.fireflysource.net.websocket.common.model.OpCode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

public abstract class CompressExtension extends AbstractExtension {

    private static final LazyLogger LOG = SystemLogger.create(CompressExtension.class);

    protected static final byte[] TAIL_BYTES = new byte[]{0x00, 0x00, (byte) 0xFF, (byte) 0xFF};
    protected static final ByteBuffer TAIL_BYTES_BUF = ByteBuffer.wrap(TAIL_BYTES);

    /**
     * Never drop tail bytes 0000FFFF, from any frame type
     */
    protected static final int TAIL_DROP_NEVER = 0;
    /**
     * Always drop tail bytes 0000FFFF, from all frame types
     */
    protected static final int TAIL_DROP_ALWAYS = 1;
    /**
     * Only drop tail bytes 0000FFFF, from fin==true frames
     */
    protected static final int TAIL_DROP_FIN_ONLY = 2;

    /**
     * Always set RSV flag, on all frame types
     */
    protected static final int RSV_USE_ALWAYS = 0;
    /**
     * Only set RSV flag on first frame in multi-frame messages.
     * <p>
     * Note: this automatically means no-continuation frames have the RSV bit set
     */
    protected static final int RSV_USE_ONLY_FIRST = 1;

    /**
     * Inflater / Decompressed Buffer Size
     */
    protected static final int INFLATE_BUFFER_SIZE = 8 * 1024;

    /**
     * Deflater / Inflater: Maximum Input Buffer Size
     */
    protected static final int INPUT_MAX_BUFFER_SIZE = 8 * 1024;

    /**
     * Inflater : Output Buffer Size
     */
    private static final int DECOMPRESS_BUF_SIZE = 8 * 1024;

    private final Queue<FrameEntry> entries = new ArrayDeque<>();
    private final IteratingCallback flusher = new Flusher();
    private Deflater deflaterImpl;
    private Inflater inflaterImpl;
    protected AtomicInteger decompressCount = new AtomicInteger(0);
    private int tailDrop = TAIL_DROP_NEVER;
    private int rsvUse = RSV_USE_ALWAYS;

    protected CompressExtension() {
        tailDrop = getTailDropMode();
        rsvUse = getRsvUseMode();
    }


    public Deflater getDeflater() {
        if (deflaterImpl == null) {
            deflaterImpl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        }
        return deflaterImpl;
    }

    public Inflater getInflater() {
        if (inflaterImpl == null) {
            inflaterImpl = new Inflater(true);
        }
        return inflaterImpl;
    }

    /**
     * Indicates use of RSV1 flag for indicating deflation is in use.
     */
    @Override
    public boolean isRsv1User() {
        return true;
    }

    /**
     * Return the mode of operation for dropping (or keeping) tail bytes in frames generated by compress (outgoing)
     *
     * @return either {@link #TAIL_DROP_ALWAYS}, {@link #TAIL_DROP_FIN_ONLY}, or {@link #TAIL_DROP_NEVER}
     */
    abstract int getTailDropMode();

    /**
     * Return the mode of operation for RSV flag use in frames generate by compress (outgoing)
     *
     * @return either {@link #RSV_USE_ALWAYS} or {@link #RSV_USE_ONLY_FIRST}
     */
    abstract int getRsvUseMode();

    protected void forwardIncoming(Frame frame, ByteAccumulator accumulator) {
        DataFrame newFrame;
        if (frame.getType() == Frame.Type.TEXT) {
            newFrame = new TextFrame(frame);
        } else {
            newFrame = new DataFrame(frame);
        }
        // Unset RSV1 since it's not compressed anymore.
        newFrame.setRsv1(false);

        ByteBuffer buffer = BufferUtils.allocate(accumulator.getLength());
        BufferUtils.flipToFill(buffer);
        accumulator.transferTo(buffer);
        newFrame.setPayload(buffer);
        nextIncomingFrame(newFrame);
    }

    protected ByteAccumulator newByteAccumulator() {
        int maxSize = Math.max(getPolicy().getMaxTextMessageSize(), getPolicy().getMaxBinaryMessageSize());
        return new ByteAccumulator(maxSize);
    }

    protected void decompress(ByteAccumulator accumulator, ByteBuffer buf) throws DataFormatException {
        if ((buf == null) || (!buf.hasRemaining())) {
            return;
        }
        byte[] output = new byte[DECOMPRESS_BUF_SIZE];

        Inflater inflater = getInflater();

        while (buf.hasRemaining() && inflater.needsInput()) {
            if (!supplyInput(inflater, buf)) {
                LOG.debug("Needed input, but no buffer could supply input");
                return;
            }

            int read;
            while ((read = inflater.inflate(output)) >= 0) {
                if (read == 0) {
                    LOG.debug("Decompress: read 0 {}", toDetail(inflater));
                    break;
                } else {
                    // do something with output
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Decompressed {} bytes: {}", read, toDetail(inflater));
                    }

                    accumulator.copyChunk(output, 0, read);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Decompress: exiting {}", toDetail(inflater));
        }
    }

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        // We use a queue and an IteratingCallback to handle concurrency.
        // We must compress and write atomically, otherwise the compression
        // context on the other end gets confused.

        if (flusher.isFailed()) {
            notifyCallbackFailure(result, new ZipException());
            return;
        }

        FrameEntry entry = new FrameEntry(frame, result);
        if (LOG.isDebugEnabled())
            LOG.debug("Queuing {}", entry);
        offerEntry(entry);
        flusher.iterate();
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

    protected void notifyCallbackSuccess(Consumer<Result<Void>> result) {
        try {
            if (result != null)
                result.accept(Result.SUCCESS);
        } catch (Throwable x) {
            if (LOG.isDebugEnabled())
                LOG.debug("Exception while notifying success", x);
        }
    }

    protected void notifyCallbackFailure(Consumer<Result<Void>> result, Throwable failure) {
        try {
            if (result != null)
                result.accept(Result.createFailedResult(failure));
        } catch (Throwable x) {
            if (LOG.isDebugEnabled())
                LOG.debug("Exception while notifying failure", x);
        }
    }

    private static boolean supplyInput(Inflater inflater, ByteBuffer buf) {
        if (buf == null || buf.remaining() <= 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No data left left to supply to Inflater");
            }
            return false;
        }

        byte[] input;
        int inputOffset;
        int len;

        if (buf.hasArray()) {
            // no need to create a new byte buffer, just return this one.
            len = buf.remaining();
            input = buf.array();
            inputOffset = buf.position() + buf.arrayOffset();
            buf.position(buf.position() + len);
        } else {
            // Only create an return byte buffer that is reasonable in size
            len = Math.min(INPUT_MAX_BUFFER_SIZE, buf.remaining());
            input = new byte[len];
            inputOffset = 0;
            buf.get(input, 0, len);
        }

        inflater.setInput(input, inputOffset, len);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Supplied {} input bytes: {}", input.length, toDetail(inflater));
        }
        return true;
    }

    private static boolean supplyInput(Deflater deflater, ByteBuffer buf) {
        if (buf == null || buf.remaining() <= 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No data left left to supply to Deflater");
            }
            return false;
        }

        byte[] input;
        int inputOffset;
        int len;

        if (buf.hasArray()) {
            // no need to create a new byte buffer, just return this one.
            len = buf.remaining();
            input = buf.array();
            inputOffset = buf.position() + buf.arrayOffset();
            buf.position(buf.position() + len);
        } else {
            // Only create an return byte buffer that is reasonable in size
            len = Math.min(INPUT_MAX_BUFFER_SIZE, buf.remaining());
            input = new byte[len];
            inputOffset = 0;
            buf.get(input, 0, len);
        }

        deflater.setInput(input, inputOffset, len);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Supplied {} input bytes: {}", input.length, toDetail(deflater));
        }
        return true;
    }

    private static String toDetail(Inflater inflater) {
        return String.format("Inflater[finished=%b,read=%d,written=%d,remaining=%d,in=%d,out=%d]", inflater.finished(), inflater.getBytesRead(),
                inflater.getBytesWritten(), inflater.getRemaining(), inflater.getTotalIn(), inflater.getTotalOut());
    }

    private static String toDetail(Deflater deflater) {
        return String.format("Deflater[finished=%b,read=%d,written=%d,in=%d,out=%d]", deflater.finished(), deflater.getBytesRead(), deflater.getBytesWritten(),
                deflater.getTotalIn(), deflater.getTotalOut());
    }

    public static boolean endsWithTail(ByteBuffer buf) {
        if ((buf == null) || (buf.remaining() < TAIL_BYTES.length)) {
            return false;
        }
        int limit = buf.limit();
        for (int i = TAIL_BYTES.length; i > 0; i--) {
            if (buf.get(limit - i) != TAIL_BYTES[TAIL_BYTES.length - i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
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
        protected Action process() throws Exception {
            if (finished) {
                current = pollEntry();
                LOG.debug("Processing {}", current);
                if (current == null)
                    return Action.IDLE;
                deflate(current);
            } else {
                compress(current, false);
            }
            return Action.SCHEDULED;
        }

        private void deflate(FrameEntry entry) {
            Frame frame = entry.frame;
            if (OpCode.isControlFrame(frame.getOpCode())) {
                // Do not deflate control frames
                nextOutgoingFrame(frame, this);
                return;
            }

            compress(entry, true);
        }

        private void compress(FrameEntry entry, boolean first) {
            // Get a chunk of the payload to avoid to blow
            // the heap if the payload is a huge mapped file.
            Frame frame = entry.frame;
            ByteBuffer data = frame.getPayload();

            if (data == null)
                data = BufferUtils.EMPTY_BUFFER;

            int remaining = data.remaining();
            int outputLength = Math.max(256, data.remaining());
            if (LOG.isDebugEnabled())
                LOG.debug("Compressing {}: {} bytes in {} bytes chunk", entry, remaining, outputLength);

            boolean needsCompress = true;

            Deflater deflater = getDeflater();

            if (deflater.needsInput() && !supplyInput(deflater, data)) {
                // no input supplied
                needsCompress = false;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] output = new byte[outputLength];

            boolean fin = frame.isFin();

            // Compress the data
            while (needsCompress) {
                int compressed = deflater.deflate(output, 0, outputLength, Deflater.SYNC_FLUSH);

                // Append the output for the eventual frame.
                if (LOG.isDebugEnabled())
                    LOG.debug("Wrote {} bytes to output buffer", compressed);
                out.write(output, 0, compressed);

                if (compressed < outputLength) {
                    needsCompress = false;
                }
            }

            ByteBuffer payload = ByteBuffer.wrap(out.toByteArray());

            if (payload.remaining() > 0) {
                // Handle tail bytes generated by SYNC_FLUSH.
                if (LOG.isDebugEnabled())
                    LOG.debug("compressed[] bytes = {}", BufferUtils.toDetailString(payload));

                if (tailDrop == TAIL_DROP_ALWAYS) {
                    if (endsWithTail(payload)) {
                        payload.limit(payload.limit() - TAIL_BYTES.length);
                    }
                    if (LOG.isDebugEnabled())
                        LOG.debug("payload (TAIL_DROP_ALWAYS) = {}", BufferUtils.toDetailString(payload));
                } else if (tailDrop == TAIL_DROP_FIN_ONLY) {
                    if (frame.isFin() && endsWithTail(payload)) {
                        payload.limit(payload.limit() - TAIL_BYTES.length);
                    }
                    if (LOG.isDebugEnabled())
                        LOG.debug("payload (TAIL_DROP_FIN_ONLY) = {}", BufferUtils.toDetailString(payload));
                }
            } else if (fin) {
                // Special case: 7.2.3.6.  Generating an Empty Fragment Manually
                // https://tools.ietf.org/html/rfc7692#section-7.2.3.6
                payload = ByteBuffer.wrap(new byte[]{0x00});
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Compressed {}: input:{} -> payload:{}", entry, outputLength, payload.remaining());
            }

            boolean continuation = frame.getType().isContinuation() || !first;
            DataFrame chunk = new DataFrame(frame, continuation);
            if (rsvUse == RSV_USE_ONLY_FIRST) {
                chunk.setRsv1(!continuation);
            } else {
                // always set
                chunk.setRsv1(true);
            }
            chunk.setPayload(payload);
            chunk.setFin(fin);

            nextOutgoingFrame(chunk, this);
        }

        @Override
        protected void onCompleteSuccess() {
            // This IteratingCallback never completes.
        }

        @Override
        protected void onCompleteFailure(Throwable x) {
            // Fail all the frames in the queue.
            FrameEntry entry;
            while ((entry = pollEntry()) != null) {
                notifyCallbackFailure(entry.result, x);
            }
        }

        @Override
        public void accept(Result<Void> result) {
            if (result.isSuccess()) {
                if (finished)
                    notifyCallbackSuccess(current.result);
            } else {
                notifyCallbackFailure(current.result, result.getThrowable());
                // If something went wrong, very likely the compression context
                // will be invalid, so we need to fail this IteratingCallback.
                LOG.warn("", result.getThrowable());
            }
            super.accept(result);
        }
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        if (deflaterImpl != null)
            deflaterImpl.end();
        if (inflaterImpl != null)
            inflaterImpl.end();
    }
}
