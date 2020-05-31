package com.fireflysource.net.websocket.model;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.frame.Frame;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Useful for testing the production of sane frame ordering from various components.
 */
public class SaneFrameOrderingAssertion implements OutgoingFrames {
    boolean priorDataFrame = false;
    public int frameCount = 0;

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        byte opcode = frame.getOpCode();
        assertTrue(OpCode.isKnown(opcode));

        switch (opcode) {
            case OpCode.TEXT:
                assertFalse(priorDataFrame, "Unexpected " + OpCode.name(opcode) + " frame, was expecting CONTINUATION");
                break;
            case OpCode.BINARY:
                assertFalse(priorDataFrame, "Unexpected " + OpCode.name(opcode) + " frame, was expecting CONTINUATION");
                break;
            case OpCode.CONTINUATION:
                assertTrue(priorDataFrame, "CONTINUATION frame without prior !FIN");
                break;
            case OpCode.CLOSE:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
            case OpCode.PING:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
            case OpCode.PONG:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
        }

        if (OpCode.isDataFrame(opcode)) {
            priorDataFrame = !frame.isFin();
        }

        frameCount++;

        if (result != null)
            result.accept(Result.SUCCESS);
    }
}
