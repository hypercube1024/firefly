package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.codec.flex.stream.Stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.Op.RECV_ES;
import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.Op.SEND_ES;

/**
 * @author Pengtao Qiu
 */
public class StreamStateTransferMap {

    private static final Map<Node, Stream.State> rules = new HashMap<>();

    static {
        Node newStream = new Node();
        newStream.op = SEND_ES;
        newStream.from = Stream.State.OPEN;
        rules.put(newStream, Stream.State.LOCALLY_CLOSED);

        newStream = new Node();
        newStream.op = RECV_ES;
        newStream.from = Stream.State.OPEN;
        rules.put(newStream, Stream.State.REMOTELY_CLOSED);

        Node locallyClosedStream = new Node();
        locallyClosedStream.op = RECV_ES;
        locallyClosedStream.from = Stream.State.LOCALLY_CLOSED;
        rules.put(locallyClosedStream, Stream.State.CLOSED);

        Node remotelyClosed = new Node();
        remotelyClosed.op = SEND_ES;
        remotelyClosed.from = Stream.State.REMOTELY_CLOSED;
        rules.put(remotelyClosed, Stream.State.CLOSED);
    }

    public static Stream.State getNextState(Stream.State currentState, Op op) {
        Node node = new Node();
        node.from = currentState;
        node.op = op;
        Stream.State state = rules.get(node);
        if (state == null) {
            throw new IllegalStateException("The stream state error. " + currentState + ", " + op);
        } else {
            return state;
        }
    }

    public enum Op {
        SEND_ES, RECV_ES
    }

    private static class Node {
        Op op;
        Stream.State from;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return op == node.op && from == node.from;
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, from);
        }
    }


}
