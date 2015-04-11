package com.firefly.codec.spdy.stream;

import java.io.File;

import com.firefly.codec.spdy.frames.control.SettingsFrame;

public class SettingsManager {

	private final File directory;
	private final String host;
	private final int port;
	
	public SettingsManager(File directory, String host, int port) {
		this.directory = directory;
		this.host = host;
		this.port = port;
	}

	public void saveSettings(SettingsFrame settingsFrame) {
		
	}
	
	public SettingsFrame loadSettings() {
		return null;
	}
	
	public void remove() {
		
	}
	
}
