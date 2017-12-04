package test.codec.websocket.model.extension.compress;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.codec.websocket.model.WriteCallback;
import test.codec.websocket.utils.Hex;

import java.util.ArrayList;
import java.util.List;

public class CapturedHexPayloads implements OutgoingFrames {
    private List<String> captured = new ArrayList<>();

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {
        String hexPayload = Hex.asHex(frame.getPayload());
        captured.add(hexPayload);
        if (callback != null) {
            callback.writeSuccess();
        }
    }

    public List<String> getCaptured() {
        return captured;
    }
}
