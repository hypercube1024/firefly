package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.AbstractParser;
import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.control.Settings;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.utils.NumberProcessUtils;
import com.firefly.net.Session;

public class SettingsBodyParser extends AbstractParser {

	public SettingsBodyParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		if(isControlFrameUnderflow(buffer, session))
			return DecodeStatus.BUFFER_UNDERFLOW;
		
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		int count = buffer.getInt();
		if(count > 0) {
			Settings settings = new Settings();
			for (int i = 0; i < count; i++) {
				Settings.Flag flag = Settings.Flag.from(buffer.get());
				Settings.ID id = Settings.ID.from(NumberProcessUtils.toUnsigned24bitsInteger(buffer.get(), buffer.getShort()));
				int value = buffer.getInt();
				Settings.Setting setting = new Settings.Setting(id, flag, value);
				settings.put(setting);
			}
			SettingsFrame settingsFrame = new SettingsFrame(
					attachment.controlFrameHeader.getVersion(), 
					attachment.controlFrameHeader.getFlags(), 
					settings);
			spdyDecodingEvent.onSettings(settingsFrame, session);
		}
		return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
	}

}
