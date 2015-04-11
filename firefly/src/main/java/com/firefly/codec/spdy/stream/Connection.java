package com.firefly.codec.spdy.stream;

import java.io.Closeable;
import java.io.IOException;
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
import com.firefly.net.Session;

public class Connection implements Closeable{
	
	private HeadersBlockParser headersBlockParser = HeadersBlockParser.newInstance();
	private HeadersBlockGenerator headersBlockGenerator = HeadersBlockGenerator.newInstance();
	private Session session;
	private NavigableSet<Stream> navigableSet = new ConcurrentSkipListSet<>();
	private Map<Integer, Stream> map = new ConcurrentHashMap<>();
	private WindowControl windowControl;
	private AtomicInteger streamIdGenerator;
	private AtomicInteger pingIdGenerator;
	private Map<Integer, PingEventListener> initiatedPing = new HashMap<>();
	public volatile SettingsFrame inboundSettingsFrame;
	public Object attachment;
	
	private final int id;
	private final boolean clientMode;
	
	public Connection(Session session, boolean clientMode) {
		this.session = session;
		this.windowControl = new WindowControl();
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

	void addStream(Stream stream) {
		map.put(stream.getId(), stream);
		navigableSet.add(stream);
	}
	
	void remove(Stream stream) {
		map.remove(stream.getId());
		navigableSet.remove(stream);
	}
	
	public Fields createFields() {
		return new Fields(new HashMap<String, Field>(), getHeadersBlockGenerator());
	}
	
	public Stream createStream(StreamEventListener streamEventListener) {
		return createStream((byte)0, streamEventListener, getInboundInitWindowSize());
	}
	
	public Stream createStream(byte priority, StreamEventListener streamEventListener) {
		return createStream(priority, streamEventListener, getInboundInitWindowSize());
	}
	
	public Stream createStream(byte priority, StreamEventListener streamEventListener, int initWindowSize) {
		if(priority < 0 || priority > 7)
			throw new IllegalArgumentException("The stream's priority is must in 0 to 7");
		if(streamEventListener == null)
			throw new IllegalArgumentException("The stream event listener is null");
		
		Stream stream = new Stream(this, generateStreamId(), priority, false, streamEventListener, initWindowSize);
		addStream(stream);
		return stream;
	}
	
	public int getInboundInitWindowSize() {
		int initWindowSize = 0;
		if(inboundSettingsFrame != null) {
			Setting setting = inboundSettingsFrame.getSettings().get(ID.INITIAL_WINDOW_SIZE);
			if(setting != null)
				initWindowSize = setting.value();
		}
		return initWindowSize;
	}
	
	public Stream getStream(int id) {
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
		PingFrame pingFrame = new PingFrame(Version.V3, generatePingId());
		session.encode(pingFrame);
		initiatedPing.put(pingFrame.getPingId(), pingEventListener);
	}
	
	synchronized void responsePing(PingFrame pingFrame) {
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

	@Override
	public void close() throws IOException {
		headersBlockGenerator.close();
		headersBlockParser.close();
		
		headersBlockParser = null;
		headersBlockGenerator = null;
		session = null;
		navigableSet = null;
		map = null;
		windowControl = null;
		streamIdGenerator = null;
		pingIdGenerator = null;
		inboundSettingsFrame = null;
		attachment = null;
		initiatedPing = null;
	}
}
