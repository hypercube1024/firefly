package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.PrefaceFrame;

public class PrefaceGenerator extends FrameGenerator {
	public PrefaceGenerator() {
		super(null);
	}

	@Override
	public List<ByteBuffer> generate(Frame frame) {
		return Collections.singletonList(ByteBuffer.wrap(PrefaceFrame.PREFACE_BYTES));
	}
}
