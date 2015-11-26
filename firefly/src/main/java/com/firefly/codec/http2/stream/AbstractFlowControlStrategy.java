package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class AbstractFlowControlStrategy implements FlowControlStrategy {
	protected static Log log = LogFactory.getInstance().getLog("firefly-system");

	private int initialStreamSendWindow;
	private int initialStreamRecvWindow;

	public AbstractFlowControlStrategy(int initialStreamSendWindow) {
		this.initialStreamSendWindow = initialStreamSendWindow;
		this.initialStreamRecvWindow = DEFAULT_WINDOW_SIZE;
	}

	protected int getInitialStreamSendWindow() {
		return initialStreamSendWindow;
	}

	protected int getInitialStreamRecvWindow() {
		return initialStreamRecvWindow;
	}

	@Override
	public void onStreamCreated(StreamSPI stream) {
		stream.updateSendWindow(initialStreamSendWindow);
		stream.updateRecvWindow(initialStreamRecvWindow);
	}

	@Override
	public void onStreamDestroyed(StreamSPI stream) {
	}

	@Override
	public void updateInitialStreamWindow(SessionSPI session, int initialStreamWindow, boolean local) {
		int previousInitialStreamWindow;
		if (local) {
			previousInitialStreamWindow = getInitialStreamRecvWindow();
			this.initialStreamRecvWindow = initialStreamWindow;
		} else {
			previousInitialStreamWindow = getInitialStreamSendWindow();
			this.initialStreamSendWindow = initialStreamWindow;
		}
		int delta = initialStreamWindow - previousInitialStreamWindow;

		// SPEC: updates of the initial window size only affect stream windows,
		// not session's.
		for (Stream stream : session.getStreams()) {
			if (local) {
				((StreamSPI) stream).updateRecvWindow(delta);
				if (log.isDebugEnable())
					log.debug("Updated initial stream recv window {} -> {} for {}", previousInitialStreamWindow,
							initialStreamWindow, stream);
			} else {
				session.onWindowUpdate((StreamSPI) stream, new WindowUpdateFrame(stream.getId(), delta));
			}
		}
	}

	@Override
	public void onWindowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame) {
		int delta = frame.getWindowDelta();
		if (frame.getStreamId() > 0) {
			// The stream may have been removed concurrently.
			if (stream != null) {
				int oldSize = stream.updateSendWindow(delta);
				if (log.isDebugEnable())
					log.debug("Updated stream send window {} -> {} for {}", oldSize, oldSize + delta, stream);
			}
		} else {
			int oldSize = session.updateSendWindow(delta);
			if (log.isDebugEnable())
				log.debug("Updated session send window {} -> {} for {}", oldSize, oldSize + delta, session);
		}
	}

	@Override
	public void onDataReceived(SessionSPI session, StreamSPI stream, int length) {
		int oldSize = session.updateRecvWindow(-length);
		if (log.isDebugEnable())
			log.debug("Data received, updated session recv window {} -> {} for {}", oldSize, oldSize - length, session);

		if (stream != null) {
			oldSize = stream.updateRecvWindow(-length);
			if (log.isDebugEnable())
				log.debug("Data received, updated stream recv window {} -> {} for {}", oldSize, oldSize - length,
						stream);
		}
	}

	@Override
	public void windowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame) {
	}

	@Override
	public void onDataSending(StreamSPI stream, int length) {
		if (length == 0)
			return;

		SessionSPI session = stream.getSession();
		int oldSize = session.updateSendWindow(-length);
		if (log.isDebugEnable())
			log.debug("Updated session send window {} -> {} for {}", oldSize, oldSize - length, session);

		oldSize = stream.updateSendWindow(-length);
		if (log.isDebugEnable())
			log.debug("Updated stream send window {} -> {} for {}", oldSize, oldSize - length, stream);
	}

	@Override
	public void onDataSent(StreamSPI stream, int length) {
	}

	@Override
	public void onSessionStalled(SessionSPI session) {
		if (log.isDebugEnable())
			log.debug("Session stalled {}", session);
	}

	@Override
	public void onStreamStalled(StreamSPI stream) {
		if (log.isDebugEnable())
			log.debug("Stream stalled {}", stream);
	}
}
