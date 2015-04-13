package com.firefly.codec.spdy.stream;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.codec.spdy.decode.control.HeadersBlockParser;
import com.firefly.codec.spdy.frames.Version;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.Fields.Field;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersBlockGenerator;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.Settings.ID;
import com.firefly.codec.spdy.frames.control.Settings.Setting;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.net.Session;

public class Connection implements Closeable{
	
	private final HeadersBlockParser headersBlockParser = HeadersBlockParser.newInstance();
	private final HeadersBlockGenerator headersBlockGenerator = HeadersBlockGenerator.newInstance();
	private final WindowControl windowControl;
	private final AtomicInteger streamIdGenerator;
	private final AtomicInteger pingIdGenerator;
	private final int id;
	private final boolean clientMode;
	private Session session;
	
	private NavigableSet<Stream> navigableSet = new ConcurrentSkipListSet<>(new Comparator<Stream>(){
		@Override
		public int compare(Stream o1, Stream o2) {
			return Byte.compare(o1.getPriority(), o2.getPriority());
		}});
	private Map<Integer, Stream> map = new ConcurrentHashMap<>();
	
	private Map<Integer, PingEventListener> initiatedPing = new HashMap<>();
	private volatile boolean isClosed = false;
	private volatile SettingsFrame inboundSettingsFrame;
	public Object attachment;
	
	public Connection(Session session, boolean clientMode) {
		this.session = session;
		this.windowControl = new WindowControl(WindowControl.DEFAULT_INITIALIZED_WINDOW_SIZE);
		this.id = session.getSessionId();
		this.clientMode = clientMode;
		streamIdGenerator = clientMode ? new AtomicInteger(1) : new AtomicInteger(2);
		pingIdGenerator = clientMode ? new AtomicInteger(1) : new AtomicInteger(2);
	}
	
	public Connection(Session session, boolean clientMode, int initWindowSize) {
		this(session, clientMode);
		windowControl.setWindowSize(initWindowSize);
	}
	
	public int getId() {
		return id;
	}
	
	public int getWindowSize() {
		return windowControl.windowSize();
	}
	
	public boolean isClientMode() {
		return clientMode;
	}

	public HeadersBlockParser getHeadersBlockParser() {
		return headersBlockParser;
	}

	public HeadersBlockGenerator getHeadersBlockGenerator() {
		return headersBlockGenerator;
	}

	private void addStream(Stream stream) {
		checkState();
		map.put(stream.getId(), stream);
		navigableSet.add(stream);
	}
	
	void remove(Stream stream) {
		checkState();
		map.remove(stream.getId());
		navigableSet.remove(stream);
	}
	
	public Fields createFields() {
		return new Fields(new HashMap<String, Field>(), getHeadersBlockGenerator());
	}
	
	public Stream createStream(StreamEventListener streamEventListener) {
		return createStream(generateStreamId(), (byte)0, false, streamEventListener, getCurrentInitWindowSize());
	}
	
	public Stream createStream(byte priority, StreamEventListener streamEventListener) {
		return createStream(generateStreamId(), priority, false, streamEventListener, getCurrentInitWindowSize());
	}
	
	Stream createStream(SynStreamFrame synStreamFrame, StreamEventListener streamEventListener) {
		return createStream(synStreamFrame.getStreamId(), synStreamFrame.getPriority(), true, streamEventListener, getCurrentInitWindowSize());
	}
	
	private Stream createStream(int streamId, byte priority, boolean isSyn, StreamEventListener streamEventListener, int initWindowSize) {
		checkState();
		if(priority < 0 || priority > 7)
			throw new IllegalArgumentException("The stream's priority is must in 0 to 7");
		if(streamEventListener == null)
			throw new IllegalArgumentException("The stream event listener is null");
		
		Stream stream = new Stream(this, streamId, priority, isSyn, streamEventListener, initWindowSize);
		addStream(stream);
		return stream;
	}
	
	private int getCurrentInitWindowSize() {
		int initWindowSize = WindowControl.DEFAULT_INITIALIZED_WINDOW_SIZE;
		if(inboundSettingsFrame != null) {
			Setting setting = inboundSettingsFrame.getSettings().get(ID.INITIAL_WINDOW_SIZE);
			if(setting != null && setting.value() > 0)
				initWindowSize = setting.value();
		}
		return initWindowSize;
	}
	
	public void setInboundSettingsFrame(SettingsFrame inboundSettingsFrame) {
		this.inboundSettingsFrame = inboundSettingsFrame;
		for(Stream stream : navigableSet) {
			stream.setCurrentInitializedWindowSize(getCurrentInitWindowSize());
		}
	}
	
	public Stream getStream(int id) {
		checkState();
		return map.get(id);
	}
	
	void updateWindow(int delta) {
		windowControl.addWindowSize(delta);
		for(Stream stream : navigableSet) {
			stream.flush();
		}
	}
	
	public void sendSettingsFrame(SettingsFrame settingsFrame) {
		session.encode(settingsFrame);
	}
	
	public synchronized void ping(PingEventListener pingEventListener) {
		checkState();
		PingFrame pingFrame = new PingFrame(Version.V3, generatePingId());
		session.encode(pingFrame);
		initiatedPing.put(pingFrame.getPingId(), pingEventListener);
	}
	
	synchronized void responsePing(PingFrame pingFrame) {
		checkState();
		if(clientMode) {
			if(isOdd(pingFrame.getPingId())) {
				PingEventListener pingEventListener = initiatedPing.get(pingFrame.getPingId());
				if(pingEventListener != null) {
					pingEventListener.onPing(pingFrame, this);
					initiatedPing.remove(pingFrame.getPingId());
				}
			} else {
				session.encode(pingFrame);
			}
		} else {
			if(isEven(pingFrame.getPingId())) {
				PingEventListener pingEventListener = initiatedPing.get(pingFrame.getPingId());
				if(pingEventListener != null) {
					pingEventListener.onPing(pingFrame, this);
					initiatedPing.remove(pingFrame.getPingId());
				}
			} else {
				session.encode(pingFrame);
			}
		}
	}
	
	private boolean isOdd(int x) {
		return (x % 2) != 0;
	}
	
	private boolean isEven(int x) {
		return (x % 2) == 0;
	}
	
	public void goAway(GoAwayFrame goAwayFrame) {
		session.encode(goAwayFrame);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (id != other.id)
			return false;
		return true;
	}

	Session getSession() {
		return session;
	}

	WindowControl getWindowControl() {
		return windowControl;
	}
	
	private int generateStreamId() {
		return streamIdGenerator.getAndAdd(2);
	}
	
	private int generatePingId() {
		return pingIdGenerator.getAndAdd(2); 
	}
	
	private void checkState() {
		if(isClosed)
			throw new IllegalStateException("The connection " + id + " has been closed");
	}

	@Override
	public void close() throws IOException {
		isClosed = true;
		headersBlockGenerator.close();
		headersBlockParser.close();
		
		session = null;
		navigableSet = null;
		map = null;
		inboundSettingsFrame = null;
		attachment = null;
		initiatedPing = null;
	}
}
