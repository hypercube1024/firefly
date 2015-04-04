package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;

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
		return "SettingsFrame [settings=" + settings + ", toString()="
				+ super.toString() + "]";
	}

	@Override
	public ByteBuffer toByteBuffer() {
		int size = settings.size();
        int frameBodyLength = 4 + 8 * size;
        int totalLength = ControlFrame.HEADER_LENGTH + frameBodyLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(frameBodyLength, buffer);
        buffer.putInt(size);
        for (Settings.Setting setting : settings){
            int id = setting.id().code();
            byte flags = setting.flag().code();
            int idAndFlags = convertIdAndFlags(id, flags);
            buffer.putInt(idAndFlags);
            buffer.putInt(setting.value());
        }
        buffer.flip();
        return buffer;
	}
	
	private int convertIdAndFlags(int id, byte flags) {
		return (flags << 24) + (id & 0xFF_FF_FF);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((settings == null) ? 0 : settings.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SettingsFrame other = (SettingsFrame) obj;
		if (settings == null) {
			if (other.settings != null)
				return false;
		} else if (!settings.equals(other.settings))
			return false;
		return true;
	}

}
