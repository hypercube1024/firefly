package com.firefly.codec.spdy.decode;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.exception.DecodingStateException;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.net.Session;
import com.firefly.utils.codec.NumberProcessUtils;

public class DataFrameParser extends AbstractParser {

	public DataFrameParser(SpdyDecodingEventListener spdyDecodingEvent) {
		super(spdyDecodingEvent);
	}

	@Override
	public DecodeStatus parse(ByteBuffer buffer, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		
		while(buffer.hasRemaining()) {
			switch (attachment.dataFrameParserState) {
			case HEAD:
				if(buffer.remaining() >= DataFrame.HEADER_LENGTH) {
					final int streamId = NumberProcessUtils.toUnsigned31bitsInteger(buffer.getInt());
					final byte flags = buffer.get();
					final int length = NumberProcessUtils.toUnsigned24bitsInteger(buffer.get(), buffer.getShort());
					
					attachment.dataFrame = new DataFrame(streamId, flags);
					attachment.dataFrame.setLength(length);
					attachment.dataFrameParserState = DataFrameParserState.BODY;
					break;
				} else {
					return DecodeStatus.BUFFER_UNDERFLOW;
				}
			case BODY:
				if(isDataFrameUnderflow(buffer, session))
					return DecodeStatus.BUFFER_UNDERFLOW;
				
				DataFrame dataFrame = null;
				if(attachment.dataFrame.getLength() > 0) {
					byte[] data = new byte[attachment.dataFrame.getLength()];
					buffer.get(data);
					
					dataFrame = new DataFrame(attachment.dataFrame.getStreamId(), attachment.dataFrame.getFlags());
					dataFrame.setData(data);
				} else {
					dataFrame = new DataFrame(attachment.dataFrame.getStreamId(), attachment.dataFrame.getFlags());
				}
				spdyDecodingEvent.onData(dataFrame, session);
				return buffer.hasRemaining() ? DecodeStatus.INIT : DecodeStatus.COMPLETE;
			default:
				throw new DecodingStateException("Data frame decoding status error");
			}
		}
		return DecodeStatus.BUFFER_UNDERFLOW;
	}

}
