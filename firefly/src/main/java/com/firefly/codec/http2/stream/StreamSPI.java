package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.utils.concurrent.Callback;

import java.io.Closeable;

/**
 * <p>The SPI interface for implementing a HTTP/2 stream.</p>
 * <p>This class extends {@link Stream} by adding the methods required to
 * implement the HTTP/2 stream functionalities.</p>
 */
public interface StreamSPI extends Stream, Closeable {

    /**
     * @return whether this stream is local or remote
     */
    public boolean isLocal();

    @Override
    public SessionSPI getSession();

    /**
     * @return the {@link com.firefly.codec.http2.stream.Stream.Listener} associated with this stream
     * @see #setListener(Stream.Listener)
     */
    public Listener getListener();

    /**
     * @param listener the {@link com.firefly.codec.http2.stream.Stream.Listener} associated with this stream
     * @see #getListener()
     */
    public void setListener(Listener listener);

    /**
     * <p>Processes the given {@code frame}, belonging to this stream.</p>
     *
     * @param frame    the frame to process
     * @param callback the callback to complete when frame has been processed
     */
    public void process(Frame frame, Callback callback);

    /**
     * <p>Updates the close state of this stream.</p>
     *
     * @param update whether to update the close state
     * @param event  the event that caused the close state update
     * @return whether the stream has been fully closed by this invocation
     */
    public boolean updateClose(boolean update, CloseState.Event event);

    /**
     * <p>Forcibly closes this stream.</p>
     */
    @Override
    public void close();

    /**
     * <p>Updates the stream send window by the given {@code delta}.</p>
     *
     * @param delta the delta value (positive or negative) to add to the stream send window
     * @return the previous value of the stream send window
     */
    public int updateSendWindow(int delta);

    /**
     * <p>Updates the stream receive window by the given {@code delta}.</p>
     *
     * @param delta the delta value (positive or negative) to add to the stream receive window
     * @return the previous value of the stream receive window
     */
    public int updateRecvWindow(int delta);

    /**
     * <p>Marks this stream as not idle so that the
     * {@link #getIdleTimeout() idle timeout} is postponed.</p>
     */
    public void notIdle();

    /**
     * @return whether the stream is closed remotely.
     * @see #isClosed()
     */
    boolean isRemotelyClosed();
}
