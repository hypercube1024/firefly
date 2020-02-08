package com.fireflysource.net.tcp.secure;

import java.nio.ByteBuffer;
import java.util.List;

public class HandshakeResult {

    private List<ByteBuffer> inAppBuffers;
    private String applicationProtocol;

    public List<ByteBuffer> getInAppBuffers() {
        return inAppBuffers;
    }

    public void setInAppBuffers(List<ByteBuffer> inAppBuffers) {
        this.inAppBuffers = inAppBuffers;
    }

    public String getApplicationProtocol() {
        return applicationProtocol;
    }

    public void setApplicationProtocol(String applicationProtocol) {
        this.applicationProtocol = applicationProtocol;
    }
}
