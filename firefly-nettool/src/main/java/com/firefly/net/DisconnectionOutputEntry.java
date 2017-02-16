package com.firefly.net;

import com.firefly.utils.concurrent.Callback;

public class DisconnectionOutputEntry extends AbstractOutputEntry<Object> {

	public DisconnectionOutputEntry(Callback callback, Object data) {
		super(callback, data);
	}

	@Override
	public OutputEntryType getOutputEntryType() {
		return OutputEntryType.DISCONNECTION;
	}

	@Override
	public long remaining() {
		return 0;
	}

}
