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

public class ControlFrameParser implements Parser {
	
	private static final Parser[] parsers = new Parser[] {
		new SynStreamBodyParser(),
		new SynReplyBodyParser(),
		new RstStreamBodyParser(),
		new SettingsBodyParser(),
		new NoOpBodyParser(),
		new PingBodyParser(),
		new GoAwayBodyParser(),
		new HeadersBodyParser(),
		new WindowUpdateBodyParser(),
		new CredentialBodyParser()};

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		while(buffer.hasRemaining()) {
			switch (attachment.controlFrameParserState) {
			case HEAD:
				if(buffer.remaining() >= ControlFrame.HEADER_LENGTH) {
					final short version = NumberProcessUtils.to15bitsShort(buffer.getShort());
					final ControlFrameType type = ControlFrameType.from(buffer.getShort());
					final byte flags = buffer.get();
					int length = NumberProcessUtils.to24bitsInteger(buffer.get(), buffer.getShort());
					
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
	
	public static void main(String[] args) {
		byte x = -1;
		System.out.println(x);
		System.out.println(0xff);
		int y = x & 0xff;
		int z = x;
		System.out.println(y);
		System.out.println(z);
		
		byte a = (byte)0xff - 1;
		System.out.println(a & 0xff);
	}

}
