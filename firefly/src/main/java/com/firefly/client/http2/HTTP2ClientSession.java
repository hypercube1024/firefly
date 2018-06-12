package com.firefly.client.http2;

import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PushPromiseFrame;
import com.firefly.codec.http2.stream.*;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HTTP2ClientSession extends HTTP2Session {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public HTTP2ClientSession(Scheduler scheduler, com.firefly.net.Session endPoint, Generator generator,
                              Listener listener, FlowControlStrategy flowControl, int streamIdleTimeout) {
        super(scheduler, endPoint, generator, listener, flowControl, 1, streamIdleTimeout);
    }

    public static HTTP2ClientSession initSessionForUpgradingHTTP2(Scheduler scheduler, com.firefly.net.Session endPoint,
                                                                  Generator generator, Listener listener, FlowControlStrategy flowControl, int initialStreamId,
                                                                  int streamIdleTimeout, final Promise<Stream> initStream, final Stream.Listener initStreamListener) {
        HTTP2ClientSession session = new HTTP2ClientSession(scheduler, endPoint, generator, listener, flowControl,
                initialStreamId, streamIdleTimeout);
        final StreamSPI stream = session.createLocalStream(1, initStream);
        stream.setListener(initStreamListener);
        stream.updateClose(true, CloseState.Event.AFTER_SEND);
        initStream.succeeded(stream);
        return session;
    }

    private HTTP2ClientSession(Scheduler scheduler, com.firefly.net.Session endPoint, Generator generator,
                               Listener listener, FlowControlStrategy flowControl, int initialStreamId, int streamIdleTimeout) {
        super(scheduler, endPoint, generator, listener, flowControl, initialStreamId, streamIdleTimeout);
    }

    @Override
    public void onHeaders(HeadersFrame frame) {
        if (log.isDebugEnabled()) {
            log.debug("Client received {}", frame);
        }
        Optional.ofNullable(getStream(frame.getStreamId()))
                .ifPresent(stream -> {
                    stream.process(frame, Callback.NOOP);
                    notifyHeaders(stream, frame);
                });
    }

    @Override
    public void onPushPromise(PushPromiseFrame frame) {
        if (log.isDebugEnabled()) {
            log.debug("Client received {}", frame);
        }

        int streamId = frame.getStreamId();
        int pushStreamId = frame.getPromisedStreamId();
        StreamSPI stream = getStream(streamId);
        if (stream == null) {
            if (log.isDebugEnabled())
                log.debug("Ignoring {}, stream #{} not found", frame, streamId);
        } else {
            StreamSPI pushStream = createRemoteStream(pushStreamId);
            pushStream.process(frame, Callback.NOOP);
            Stream.Listener listener = notifyPush(stream, pushStream, frame);
            pushStream.setListener(listener);
        }
    }

    private Stream.Listener notifyPush(StreamSPI stream, StreamSPI pushStream, PushPromiseFrame frame) {
        Stream.Listener listener = stream.getListener();
        if (listener == null)
            return null;
        try {
            return listener.onPush(pushStream, frame);
        } catch (Throwable x) {
            log.error("Failure while notifying listener {}", x, listener);
            return null;
        }
    }
}
