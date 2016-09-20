package com.firefly.net.tcp.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import com.firefly.utils.function.Action1;

public class CharParser implements MessageHandler<ByteBuffer, String> {

	protected ByteBuffer buffer;
	protected Action1<String> complete;
	private CharsetDecoder decoder;

	public CharParser() {
		this("UTF-8");
	}

	public CharParser(String charset) {
		decoder = Charset.forName(charset).newDecoder();
	}

	@Override
	public void complete(Action1<String> complete) {
		this.complete = complete;
	}

	@Override
	public void receive(ByteBuffer buf) {
		merge(buf);
		parse();
	}

	public void merge(ByteBuffer buf) {
		if (buffer != null) {
			if (buffer.hasRemaining()) {
				ByteBuffer tmp = ByteBuffer.allocate(buffer.remaining() + buf.remaining());
				tmp.put(buffer).put(buf).flip();
				buffer = tmp;
			} else {
				buffer = buf;
			}
		} else {
			buffer = buf;
		}
	}

	protected void parse() {
		CharBuffer charBuff = allocate();
		while (buffer.hasRemaining()) {
			CoderResult r = decoder.decode(buffer, charBuff, false);
			charBuff.flip();
			if (r.isUnderflow()) {
				if (buffer.hasRemaining()) {
					buffer = buffer.slice();
				}
				if (charBuff.hasRemaining()) {
					complete.call(charBuff.toString());
				}
				break;
			} else if (r.isOverflow()) {
				complete.call(charBuff.toString());
				charBuff = allocate();
			}
		}
	}

	protected CharBuffer allocate() {
		int expectedLength = (int) (buffer.remaining() * decoder.averageCharsPerByte()) + 1;
		CharBuffer charBuff = CharBuffer.allocate(expectedLength);
		return charBuff;
	}

}
