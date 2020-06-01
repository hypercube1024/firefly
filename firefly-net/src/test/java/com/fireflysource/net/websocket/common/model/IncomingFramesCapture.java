package com.fireflysource.net.websocket.common.model;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.frame.WebSocketFrame;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class IncomingFramesCapture implements IncomingFrames {
    private LinkedBlockingQueue<WebSocketFrame> frames = new LinkedBlockingQueue<>();

    public void assertFrameCount(int expectedCount) {
        if (frames.size() != expectedCount) {
            // dump details
            System.err.printf("Expected %d frame(s)%n", expectedCount);
            System.err.printf("But actually captured %d frame(s)%n", frames.size());
            int i = 0;
            for (Frame frame : frames) {
                System.err.printf(" [%d] Frame[%s] - %s%n", i++,
                        OpCode.name(frame.getOpCode()),
                        BufferUtils.toDetailString(frame.getPayload()));
            }
        }
        assertEquals(expectedCount, frames.size());
    }

    public void assertHasFrame(byte op) {
        assertTrue(op >= 1);
    }

    public void assertHasFrame(byte op, int expectedCount) {
        String msg = String.format("%s frame count", OpCode.name(op));
        assertEquals(expectedCount, getFrameCount(op));
    }

    public void assertHasNoFrames() {
        assertEquals(0, frames.size());
    }

    public void clear() {
        frames.clear();
    }

    public void dump() {
        System.err.printf("Captured %d incoming frames%n", frames.size());
        int i = 0;
        for (Frame frame : frames) {
            System.err.printf("[%3d] %s%n", i++, frame);
            System.err.printf("          payload: %s%n", BufferUtils.toDetailString(frame.getPayload()));
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

    public Queue<WebSocketFrame> getFrames() {
        return frames;
    }

    @Override
    public void incomingFrame(Frame frame) {
        WebSocketFrame copy = WebSocketFrame.copy(frame);
        // TODO: might need to make this optional (depending on use by client vs server tests)
        // assertThat("frame.masking must be set",frame.isMasked(),is(true));
        frames.add(copy);
    }

    public int size() {
        return frames.size();
    }
}
