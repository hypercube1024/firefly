package test.codec.websocket.model.extension.compress;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.utils.concurrent.Callback;
import test.codec.websocket.utils.Hex;

import java.util.ArrayList;
import java.util.List;

public class CapturedHexPayloads implements OutgoingFrames {
    private List<String> captured = new ArrayList<>();

    @Override
    public void outgoingFrame(Frame frame, Callback callback) {
        String hexPayload = Hex.asHex(frame.getPayload());
        captured.add(hexPayload);
        if (callback != null) {
            callback.succeeded();
        }
    }

    public List<String> getCaptured() {
        return captured;
    }
}
