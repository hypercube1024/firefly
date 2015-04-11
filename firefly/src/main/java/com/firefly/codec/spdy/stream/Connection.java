package com.firefly.codec.spdy.stream;

import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.Settings.ID;
import com.firefly.codec.spdy.frames.control.Settings.Setting;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.net.Session;

public class Connection {
	private final Session session;
	private final WindowControl windowControl;
	private final int id;
	private final NavigableSet<Stream> navigableSet = new ConcurrentSkipListSet<>();
	private final Map<Integer, Stream> map = new ConcurrentHashMap<>();
	private final boolean clientMode;
	private AtomicInteger streamIdGenerator;
	public volatile SettingsFrame inboundSettingsFrame;
	
	public Connection(Session session, boolean clientMode) {
		this.session = session;
		this.windowControl = new WindowControl();
		this.id = session.getSessionId();
		this.clientMode = clientMode;
		streamIdGenerator = clientMode ? new AtomicInteger(1) : new AtomicInteger(2);
	}
	
	public Connection(Session session, boolean clientMode, int initWindowSize) {
		this(session, clientMode);
		windowControl.setWindowSize(initWindowSize);
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isClientMode() {
		return clientMode;
	}

	public void addStream(Stream stream) {
		map.put(stream.getId(), stream);
		navigableSet.add(stream);
	}
	
	public void remove(Stream stream) {
		map.remove(stream.getId());
		navigableSet.remove(stream);
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
	
	public void updateWindow(int delta) {
		windowControl.addWindowSize(delta);
		for(Stream stream : navigableSet) {
			stream.flush();
		}
	}
	
	public void sendSettingsFrame(SettingsFrame settingsFrame) {
		session.encode(settingsFrame);
	}
	
	public void ping(PingFrame pingFrame) {
		session.encode(pingFrame);
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
}
