package com.firefly.codec.http2.stream;

import java.util.concurrent.atomic.AtomicLong;

import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class AbstractFlowControlStrategy implements FlowControlStrategy {
	protected static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final AtomicLong sessionStalls = new AtomicLong();
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
				if (log.isDebugEnabled())
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
				if (log.isDebugEnabled())
					log.debug("Updated stream send window {} -> {} for {}", oldSize, oldSize + delta, stream);
				if (oldSize <= 0)
					onStreamUnstalled(stream);
			}
		} else {
			int oldSize = session.updateSendWindow(delta);
			if (log.isDebugEnabled())
				log.debug("Updated session send window {} -> {} for {}", oldSize, oldSize + delta, session);
			if (oldSize <= 0)
				onSessionUnstalled(session);
		}
	}

	@Override
	public void onDataReceived(SessionSPI session, StreamSPI stream, int length) {
		int oldSize = session.updateRecvWindow(-length);
		if (log.isDebugEnabled())
			log.debug("Data received, updated session recv window {} -> {} for {}", oldSize, oldSize - length, session);

		if (stream != null) {
			oldSize = stream.updateRecvWindow(-length);
			if (log.isDebugEnabled())
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
		int oldSessionWindow = session.updateSendWindow(-length);
		int newSessionWindow = oldSessionWindow - length;
		if (log.isDebugEnabled())
			log.debug("Sending, session send window {} -> {} for {}", oldSessionWindow, newSessionWindow, session);
		if (newSessionWindow <= 0)
			onSessionStalled(session);

		int oldStreamWindow = stream.updateSendWindow(-length);
		int newStreamWindow = oldStreamWindow - length;
		if (log.isDebugEnabled())
			log.debug("Sending, stream send window {} -> {} for {}", oldStreamWindow, newStreamWindow, stream);
		if (newStreamWindow <= 0)
			onStreamStalled(stream);
	}

	@Override
	public void onDataSent(StreamSPI stream, int length) {
	}

	protected void onSessionStalled(SessionSPI session) {
		if (log.isDebugEnabled())
			log.debug("Session stalled {}", session);

		sessionStalls.incrementAndGet();
	}

	protected void onStreamStalled(StreamSPI stream) {
		if (log.isDebugEnabled())
			log.debug("Stream stalled {}", stream);
	}

	protected void onSessionUnstalled(SessionSPI session) {
		if (log.isDebugEnabled())
			log.debug("Session unstalled {}", session);
	}

	protected void onStreamUnstalled(StreamSPI stream) {
		if (log.isDebugEnabled())
			log.debug("Stream unstalled {}", stream);
	}

	public long getSessionStallCount() {
		return sessionStalls.get();
	}

	public void reset() {
		sessionStalls.set(0);
	}
}
