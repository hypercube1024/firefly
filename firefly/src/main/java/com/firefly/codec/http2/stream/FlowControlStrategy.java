package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.WindowUpdateFrame;

public interface FlowControlStrategy {
	public static int DEFAULT_WINDOW_SIZE = 65535;

	public void onStreamCreated(StreamSPI stream);

	public void onStreamDestroyed(StreamSPI stream);

	public void updateInitialStreamWindow(SessionSPI session, int initialStreamWindow, boolean local);

	public void onWindowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame);

	public void onDataReceived(SessionSPI session, StreamSPI stream, int length);

	public void onDataConsumed(SessionSPI session, StreamSPI stream, int length);

	public void windowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame);

	public void onDataSending(StreamSPI stream, int length);

	public void onDataSent(StreamSPI stream, int length);
}
