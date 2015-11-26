package com.firefly.codec.http2.stream;

import java.io.Closeable;

import com.firefly.codec.common.Callback;
import com.firefly.codec.http2.frame.Frame;

/**
 * <p>
 * The SPI interface for implementing a HTTP/2 stream.
 * </p>
 * <p>
 * This class extends {@link Stream} by adding the methods required to implement
 * the HTTP/2 stream functionalities.
 * </p>
 */
public interface StreamSPI extends Stream, Closeable {
	/**
	 * <p>
	 * The constant used as attribute key to store/retrieve the HTTP channel
	 * associated with this stream
	 * </p>
	 *
	 * @see #setAttribute(String, Object)
	 */
	public static final String CHANNEL_ATTRIBUTE = StreamSPI.class.getName() + ".channel";
	
	/**
     * @return whether this stream is local or remote
     */
    public boolean isLocal();

	@Override
	public SessionSPI getSession();

	/**
	 * @return the {@link org.eclipse.jetty.http2.api.Stream.Listener}
	 *         associated with this stream
	 * @see #setListener(Listener)
	 */
	public Listener getListener();

	/**
	 * @param listener
	 *            the {@link org.eclipse.jetty.http2.api.Stream.Listener}
	 *            associated with this stream
	 * @see #getListener()
	 */
	public void setListener(Listener listener);

	/**
	 * <p>
	 * Processes the given {@code frame}, belonging to this stream.
	 * </p>
	 *
	 * @param frame
	 *            the frame to process
	 * @param callback
	 *            the callback to complete when frame has been processed
	 */
	public void process(Frame frame, Callback callback);

	/**
	 * <p>
	 * Updates the close state of this stream.
	 * </p>
	 *
	 * @param update
	 *            whether to update the close state
	 * @param local
	 *            whether the update comes from a local operation (such as
	 *            sending a frame that ends the stream) or a remote operation
	 *            (such as receiving a frame
	 * @return whether the stream has been fully closed by this invocation
	 */
	public boolean updateClose(boolean update, boolean local);

	/**
	 * <p>
	 * Forcibly closes this stream.
	 * </p>
	 */
	@Override
	public void close();

	/**
	 * <p>
	 * Updates the stream send window by the given {@code delta}.
	 * </p>
	 *
	 * @param delta
	 *            the delta value (positive or negative) to add to the stream
	 *            send window
	 * @return the previous value of the stream send window
	 */
	public int updateSendWindow(int delta);

	/**
	 * <p>
	 * Updates the stream receive window by the given {@code delta}.
	 * </p>
	 *
	 * @param delta
	 *            the delta value (positive or negative) to add to the stream
	 *            receive window
	 * @return the previous value of the stream receive window
	 */
	public int updateRecvWindow(int delta);
}
