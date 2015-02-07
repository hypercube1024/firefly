package com.firefly.codec.spdy.frames.control;

import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;

public class SettingsFrame extends ControlFrame {
	
	public static final byte CLEAR_PERSISTED = 1;
	
	private final Settings settings;

	public SettingsFrame(short version, byte flags, Settings settings) {
		super(version, ControlFrameType.SETTINGS, flags);
		this.settings = settings;
	}

	public boolean isClearPersisted() {
		return (getFlags() & CLEAR_PERSISTED) == CLEAR_PERSISTED;
	}

	public Settings getSettings() {
		return settings;
	}

	@Override
	public String toString() {
		return "SettingsFrame [settings=" + settings + "]";
	}

}
