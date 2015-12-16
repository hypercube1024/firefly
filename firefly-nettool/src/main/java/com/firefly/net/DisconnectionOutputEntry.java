package com.firefly.net;

import com.firefly.net.tcp.aio.AbstractOutputEntry;
import com.firefly.utils.concurrent.Callback;

public class DisconnectionOutputEntry extends AbstractOutputEntry<Object> {

	public DisconnectionOutputEntry(Callback callback, Object data) {
		super(callback, data);
	}

	@Override
	public OutputEntryType getOutputEntryType() {
		return OutputEntryType.DISCONNECTION;
	}

}
