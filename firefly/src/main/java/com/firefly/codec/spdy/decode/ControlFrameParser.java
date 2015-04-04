package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.control.ControlFrameHeader;
import com.firefly.codec.spdy.decode.control.CredentialBodyParser;
import com.firefly.codec.spdy.decode.control.GoAwayBodyParser;
import com.firefly.codec.spdy.decode.control.HeadersBodyParser;
import com.firefly.codec.spdy.decode.control.NoOpBodyParser;
import com.firefly.codec.spdy.decode.control.PingBodyParser;
import com.firefly.codec.spdy.decode.control.RstStreamBodyParser;
import com.firefly.codec.spdy.decode.control.SettingsBodyParser;
import com.firefly.codec.spdy.decode.control.SynReplyBodyParser;
import com.firefly.codec.spdy.decode.control.SynStreamBodyParser;
import com.firefly.codec.spdy.decode.control.WindowUpdateBodyParser;
import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.codec.spdy.decode.utils.NumberProcessUtils;
import com.firefly.codec.spdy.frames.ControlFrame;
import com.firefly.codec.spdy.frames.ControlFrameType;
import com.firefly.net.Session;

public class ControlFrameParser extends AbstractParser {
	
	private final Parser[] parsers;
	
	public ControlFrameParser(SpdyDecodingEvent spdyDecodingEvent) {
		super(spdyDecodingEvent);
		parsers = new Parser[] {
				new SynStreamBodyParser(spdyDecodingEvent),
				new SynReplyBodyParser(spdyDecodingEvent),
				new RstStreamBodyParser(spdyDecodingEvent),
				new SettingsBodyParser(spdyDecodingEvent),
				new NoOpBodyParser(spdyDecodingEvent),
				new PingBodyParser(spdyDecodingEvent),
				new GoAwayBodyParser(spdyDecodingEvent),
				new HeadersBodyParser(spdyDecodingEvent),
				new WindowUpdateBodyParser(spdyDecodingEvent),
				new CredentialBodyParser(spdyDecodingEvent)};
	}	

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		while(buffer.hasRemaining()) {
			switch (attachment.controlFrameParserState) {
			case HEAD:
				if(buffer.remaining() >= ControlFrame.HEADER_LENGTH) {
					final short version = NumberProcessUtils.toUnsigned15bitsShort(buffer.getShort());
					final ControlFrameType type = ControlFrameType.from(buffer.getShort());
					final byte flags = buffer.get();
					final int length = NumberProcessUtils.toUnsigned24bitsInteger(buffer.get(), buffer.getShort());
					
					attachment.controlFrameHeader = new ControlFrameHeader(version, type, flags, length);
					attachment.controlFrameParserState = ControlFrameParserState.BODY;
					break;
				} else {
					return DecodeStatus.BUFFER_UNDERFLOW;
				}
			case BODY:
				return getParser(attachment.controlFrameHeader.getType()).parse(buffer, session);
			default:
				throw new DecodingStateException("Control frame decoding status error");
			}
		}
		return DecodeStatus.BUFFER_UNDERFLOW;
	}
	
	private Parser getParser(ControlFrameType frameType) {
		return parsers[frameType.getCode() - 1];
	}

}
