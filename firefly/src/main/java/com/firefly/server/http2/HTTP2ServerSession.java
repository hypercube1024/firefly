package com.firefly.server.http2;

import com.firefly.codec.http2.decode.ServerParser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.*;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class HTTP2ServerSession extends HTTP2Session implements ServerParser.Listener {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final ServerSessionListener listener;

    public HTTP2ServerSession(Scheduler scheduler, com.firefly.net.Session endPoint, Generator generator,
                              ServerSessionListener listener, FlowControlStrategy flowControl, int streamIdleTimeout) {
        super(scheduler, endPoint, generator, listener, flowControl, 2, streamIdleTimeout);
        this.listener = listener;
    }

    @Override
    public void onPreface() {
        // SPEC: send a SETTINGS frame upon receiving the preface.
        Map<Integer, Integer> settings = notifyPreface(this);
        if (settings == null)
            settings = Collections.emptyMap();
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);

        WindowUpdateFrame windowFrame = null;
        int sessionWindow = getInitialSessionRecvWindow() - FlowControlStrategy.DEFAULT_WINDOW_SIZE;
        if (sessionWindow > 0) {
            updateRecvWindow(sessionWindow);
            windowFrame = new WindowUpdateFrame(0, sessionWindow);
        }

        if (windowFrame == null)
            frames(null, Callback.NOOP, settingsFrame, Frame.EMPTY_ARRAY);
        else
            frames(null, Callback.NOOP, settingsFrame, windowFrame);
    }

    @Override
    public void onHeaders(HeadersFrame frame) {
        if (log.isDebugEnabled()) {
            log.debug("Server received {}", frame);
        }

        MetaData metaData = frame.getMetaData();
        if (metaData.isRequest()) {
            StreamSPI stream = createRemoteStream(frame.getStreamId());
            if (stream != null) {
                stream.process(frame, Callback.NOOP);
                Stream.Listener listener = notifyNewStream(stream, frame);
                stream.setListener(listener);
            }
        } else {
            if (frame.isEndStream()) { // The trailer frame
                Optional.ofNullable(getStream(frame.getStreamId()))
                        .ifPresent(stream -> {
                            stream.process(frame, Callback.NOOP);
                            notifyHeaders(stream, frame);
                        });
            } else {
                onConnectionFailure(ErrorCode.INTERNAL_ERROR.code, "invalid_request");
            }
        }
    }

    @Override
    public void onPushPromise(PushPromiseFrame frame) {
        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "push_promise");
    }

    private Map<Integer, Integer> notifyPreface(Session session) {
        try {
            return listener.onPreface(session);
        } catch (Throwable x) {
            log.error("Failure while notifying listener {}", x, listener);
            return null;
        }
    }

    @Override
    public void onFrame(Frame frame) {
        switch (frame.getType()) {
            case PREFACE:
                onPreface();
                break;
            case SETTINGS:
                // SPEC: the required reply to this SETTINGS frame is the 101
                // response.
                onSettings((SettingsFrame) frame, false);
                break;
            case HEADERS:
                onHeaders((HeadersFrame) frame);
                break;
            default:
                super.onFrame(frame);
                break;
        }
    }
}
