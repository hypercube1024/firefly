package com.firefly.server.http2;

import java.util.Map;

import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;

public class HTTP2ServerRequestHandler implements ServerSessionListener {
	
	private ServerHTTPHandler serverHTTPHandler;

	public HTTP2ServerRequestHandler(ServerHTTPHandler serverHTTPHandler) {
		this.serverHTTPHandler = serverHTTPHandler;
	}

	@Override
	public Map<Integer, Integer> onPreface(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Listener onNewStream(Stream stream, HeadersFrame frame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onSettings(Session session, SettingsFrame frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPing(Session session, PingFrame frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReset(Session session, ResetFrame frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClose(Session session, GoAwayFrame frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onIdleTimeout(Session session) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onFailure(Session session, Throwable failure) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccept(Session session) {
		// TODO Auto-generated method stub
		
	}

}
