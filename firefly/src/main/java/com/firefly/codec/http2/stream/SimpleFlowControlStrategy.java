package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.utils.concurrent.Callback;

public class SimpleFlowControlStrategy extends AbstractFlowControlStrategy {
	public SimpleFlowControlStrategy() {
		this(DEFAULT_WINDOW_SIZE);
	}

	public SimpleFlowControlStrategy(int initialStreamSendWindow) {
		super(initialStreamSendWindow);
	}

	@Override
	public void onDataConsumed(SessionSPI session, StreamSPI stream, int length) {
		if (length <= 0)
			return;

		// This is the simple algorithm for flow control.
		// This method is called when a whole flow controlled frame has been
		// consumed.
		// We send a WindowUpdate every time, even if the frame was very small.

		WindowUpdateFrame sessionFrame = new WindowUpdateFrame(0, length);
		session.updateRecvWindow(length);
		if (log.isDebugEnable())
			log.debug("Data consumed, increased session recv window by {} for {}", length, session);

		Frame[] streamFrame = Frame.EMPTY_ARRAY;
		if (stream != null) {
			if (stream.isClosed()) {
				if (log.isDebugEnable())
					log.debug("Data consumed, ignoring update stream recv window by {} for closed {}", length, stream);
			} else {
				streamFrame = new Frame[1];
				streamFrame[0] = new WindowUpdateFrame(stream.getId(), length);
				stream.updateRecvWindow(length);
				if (log.isDebugEnable())
					log.debug("Data consumed, increased stream recv window by {} for {}", length, stream);
			}
		}

		session.frames(stream, Callback.NOOP, sessionFrame, streamFrame);
	}
}
