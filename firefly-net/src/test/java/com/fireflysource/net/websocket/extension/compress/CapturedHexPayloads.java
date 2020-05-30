package com.fireflysource.net.websocket.extension.compress;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.frame.Frame;
import com.fireflysource.net.websocket.model.OutgoingFrames;
import com.fireflysource.net.websocket.utils.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CapturedHexPayloads implements OutgoingFrames {
    private List<String> captured = new ArrayList<>();

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        String hexPayload = Hex.asHex(frame.getPayload());
        captured.add(hexPayload);
        if (result != null) {
            result.accept(Result.SUCCESS);
        }
    }

    public List<String> getCaptured() {
        return captured;
    }
}
