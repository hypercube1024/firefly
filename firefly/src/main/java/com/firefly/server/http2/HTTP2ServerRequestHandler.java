package com.firefly.server.http2;

import java.util.Map;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.stream.Session;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.codec.http2.stream.Stream.Listener;
import com.firefly.utils.concurrent.Callback;

public class HTTP2ServerRequestHandler implements ServerSessionListener {
	
	private final ServerHTTPHandler serverHTTPHandler;
	HTTP2ServerConnection connection;

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
		
		return new Listener(){

			@Override
			public void onHeaders(Stream stream, HeadersFrame frame) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Listener onPush(Stream stream, PushPromiseFrame frame) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void onData(Stream stream, DataFrame frame, Callback callback) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onReset(Stream stream, ResetFrame frame) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTimeout(Stream stream, Throwable x) {
				// TODO Auto-generated method stub
				
			}};
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
