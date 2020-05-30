package com.fireflysource.net.websocket.model;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.frame.Frame;
import com.fireflysource.net.websocket.frame.WebSocketFrame;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutgoingFramesCapture implements OutgoingFrames {
    private LinkedBlockingDeque<WebSocketFrame> frames = new LinkedBlockingDeque<>();

    public void assertFrameCount(int expectedCount) {
        assertEquals(expectedCount, frames.size());
    }

    public void assertHasFrame(byte op) {
        assertTrue(getFrameCount(op) >= 1);
    }

    public void assertHasFrame(byte op, int expectedCount) {
        assertEquals(expectedCount, getFrameCount(op));
    }

    public void assertHasNoFrames() {
        assertEquals(0, frames.size());
    }

    public void dump() {
        System.out.printf("Captured %d outgoing writes%n", frames.size());
        int i = 0;
        for (WebSocketFrame frame : frames) {
            System.out.printf("[%3d] %s%n", i, frame);
            System.out.printf("      %s%n", BufferUtils.toDetailString(frame.getPayload()));
            i++;
        }
    }

    public int getFrameCount(byte op) {
        int count = 0;
        for (WebSocketFrame frame : frames) {
            if (frame.getOpCode() == op) {
                count++;
            }
        }
        return count;
    }

    public LinkedBlockingDeque<WebSocketFrame> getFrames() {
        return frames;
    }

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        frames.add(WebSocketFrame.copy(frame));
        if (result != null) {
            result.accept(Result.SUCCESS);
        }
    }
}
