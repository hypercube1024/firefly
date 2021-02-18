package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.SearchPattern;
import com.fireflysource.common.string.Utf8StringBuilder;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.exception.BadMessageException;
import com.fireflysource.net.http.common.model.HttpTokens;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * A parser for MultiPart content type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc2046#section-5.1">https://tools.ietf.org/html/rfc2046#section-5.1</a>
 * @see <a href="https://tools.ietf.org/html/rfc2045">https://tools.ietf.org/html/rfc2045</a>
 */
public class MultiPartParser {
    public static final LazyLogger LOG = SystemLogger.create(MultiPartParser.class);

    // States
    public enum FieldState {
        FIELD,
        IN_NAME,
        AFTER_NAME,
        VALUE,
        IN_VALUE
    }

    // States
    public enum State {
        PREAMBLE,
        DELIMITER,
        DELIMITER_PADDING,
        DELIMITER_CLOSE,
        BODY_PART,
        FIRST_OCTETS,
        OCTETS,
        EPILOGUE,
        END
    }

    private static final EnumSet<State> DELIMITER_STATES = EnumSet.of(State.DELIMITER, State.DELIMITER_CLOSE, State.DELIMITER_PADDING);
    private static final int MAX_HEADER_LINE_LENGTH = 998;

    private final Handler handler;
    private final SearchPattern delimiterSearch;

    private String fieldName;
    private String fieldValue;

    private State state = State.PREAMBLE;
    private FieldState fieldState = FieldState.FIELD;
    private int partialBoundary = 2; // No CRLF if no preamble
    private boolean cr;
    private ByteBuffer patternBuffer;

    private final Utf8StringBuilder string = new Utf8StringBuilder();
    private int length;

    private int totalHeaderLineLength = -1;

    public MultiPartParser(Handler handler, String boundary) {
        this.handler = handler;

        String delimiter = "\r\n--" + boundary;
        patternBuffer = ByteBuffer.wrap(delimiter.getBytes(StandardCharsets.US_ASCII));
        delimiterSearch = SearchPattern.compile(patternBuffer.array());
    }

    public void reset() {
        state = State.PREAMBLE;
        fieldState = FieldState.FIELD;
        partialBoundary = 2; // No CRLF if no preamble
    }

    public Handler getHandler() {
        return handler;
    }

    public State getState() {
        return state;
    }

    public boolean isState(State state) {
        return this.state == state;
    }

    private static boolean hasNextByte(ByteBuffer buffer) {
        return BufferUtils.hasContent(buffer);
    }

    private HttpTokens.Token next(ByteBuffer buffer) {
        byte ch = buffer.get();
        HttpTokens.Token t = HttpTokens.TOKENS[0xff & ch];

        switch (t.getType()) {
            case CNTL:
                throw new IllegalCharacterException(state, t, buffer);

            case LF:
                cr = false;
                break;

            case CR:
                if (cr)
                    throw new BadMessageException("Bad EOL");

                cr = true;
                return null;

            case ALPHA:
            case DIGIT:
            case TCHAR:
            case VCHAR:
            case HTAB:
            case SPACE:
            case OTEXT:
            case COLON:
                if (cr)
                    throw new BadMessageException("Bad EOL");
                break;

            default:
                break;
        }

        return t;
    }

    private void setString(String s) {
        string.reset();
        string.append(s);
        length = s.length();
    }

    /*
     * Mime Field strings are treated as UTF-8 as per https://tools.ietf.org/html/rfc7578#section-5.1
     */
    private String takeString() {
        String s = string.toString();
        // trim trailing whitespace.
        if (s.length() > length)
            s = s.substring(0, length);
        string.reset();
        length = -1;
        return s;
    }

    /**
     * Parse until next Event.
     *
     * @param buffer the buffer to parse
     * @param last   whether this buffer contains last bit of content
     * @return True if an RequestHandler method called and it returned true;
     */
    public boolean parse(ByteBuffer buffer, boolean last) {
        boolean handle = false;
        while (!handle && BufferUtils.hasContent(buffer)) {
            switch (state) {
                case PREAMBLE:
                    parsePreamble(buffer);
                    continue;

                case DELIMITER:
                case DELIMITER_PADDING:
                case DELIMITER_CLOSE:
                    parseDelimiter(buffer);
                    continue;

                case BODY_PART:
                    handle = parseMimePartHeaders(buffer);
                    break;

                case FIRST_OCTETS:
                case OCTETS:
                    handle = parseOctetContent(buffer);
                    break;

                case EPILOGUE:
                    BufferUtils.clear(buffer);
                    break;

                case END:
                    handle = true;
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        if (last && BufferUtils.isEmpty(buffer)) {
            if (state == State.EPILOGUE) {
                state = State.END;

                if (LOG.isDebugEnabled())
                    LOG.debug("messageComplete {}", this);

                return handler.messageComplete();
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("earlyEOF {}", this);

                handler.earlyEOF();
                return true;
            }
        }

        return handle;
    }

    private void parsePreamble(ByteBuffer buffer) {
        if (LOG.isDebugEnabled())
            LOG.debug("parsePreamble({})", BufferUtils.toDetailString(buffer));

        if (partialBoundary > 0) {
            int partial = delimiterSearch.startsWith(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), partialBoundary);
            if (partial > 0) {
                if (partial == delimiterSearch.getLength()) {
                    buffer.position(buffer.position() + partial - partialBoundary);
                    partialBoundary = 0;
                    setState(State.DELIMITER);
                    return;
                }

                partialBoundary = partial;
                BufferUtils.clear(buffer);
                return;
            }

            partialBoundary = 0;
        }

        int delimiter = delimiterSearch.match(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        if (delimiter >= 0) {
            buffer.position(delimiter - buffer.arrayOffset() + delimiterSearch.getLength());
            setState(State.DELIMITER);
            return;
        }

        partialBoundary = delimiterSearch.endsWith(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        BufferUtils.clear(buffer);
    }

    private void parseDelimiter(ByteBuffer buffer) {
        if (LOG.isDebugEnabled())
            LOG.debug("parseDelimiter({})", BufferUtils.toDetailString(buffer));

        while (DELIMITER_STATES.contains(state) && hasNextByte(buffer)) {
            HttpTokens.Token t = next(buffer);
            if (t == null)
                return;

            if (t.getType() == HttpTokens.Type.LF) {
                setState(State.BODY_PART);

                if (LOG.isDebugEnabled())
                    LOG.debug("startPart {}", this);

                handler.startPart();
                return;
            }

            switch (state) {
                case DELIMITER:
                    if (t.getChar() == '-')
                        setState(State.DELIMITER_CLOSE);
                    else
                        setState(State.DELIMITER_PADDING);
                    continue;

                case DELIMITER_CLOSE:
                    if (t.getChar() == '-') {
                        setState(State.EPILOGUE);
                        return;
                    }
                    setState(State.DELIMITER_PADDING);
                    continue;

                case DELIMITER_PADDING:
                default:
            }
        }
    }

    /*
     * Parse the message headers and return true if the handler has signaled for a return
     */
    protected boolean parseMimePartHeaders(ByteBuffer buffer) {
        if (LOG.isDebugEnabled())
            LOG.debug("parseMimePartHeaders({})", BufferUtils.toDetailString(buffer));

        // Process headers
        while (state == State.BODY_PART && hasNextByte(buffer)) {
            // process each character
            HttpTokens.Token t = next(buffer);
            if (t == null)
                break;

            if (t.getType() != HttpTokens.Type.LF)
                totalHeaderLineLength++;

            if (totalHeaderLineLength > MAX_HEADER_LINE_LENGTH)
                throw new IllegalStateException("Header Line Exceeded Max Length");

            switch (fieldState) {
                case FIELD:
                    switch (t.getType()) {
                        case SPACE:
                        case HTAB: {
                            // Folded field value!

                            if (fieldName == null)
                                throw new IllegalStateException("First field folded");

                            if (fieldValue == null) {
                                string.reset();
                                length = 0;
                            } else {
                                setString(fieldValue);
                                string.append(' ');
                                length++;
                                fieldValue = null;
                            }
                            setState(FieldState.VALUE);
                            break;
                        }

                        case LF:
                            handleField();
                            setState(State.FIRST_OCTETS);
                            partialBoundary = 2; // CRLF is option for empty parts

                            if (LOG.isDebugEnabled())
                                LOG.debug("headerComplete {}", this);

                            if (handler.headerComplete())
                                return true;
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                            // process previous header
                            handleField();

                            // New header
                            setState(FieldState.IN_NAME);
                            string.reset();
                            string.append(t.getChar());
                            length = 1;

                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case IN_NAME:
                    switch (t.getType()) {
                        case COLON:
                            fieldName = takeString();
                            length = -1;
                            setState(FieldState.VALUE);
                            break;

                        case SPACE:
                            // Ignore trailing whitespaces
                            setState(FieldState.AFTER_NAME);
                            break;

                        case LF: {
                            if (LOG.isDebugEnabled())
                                LOG.debug("Line Feed in Name {}", this);

                            handleField();
                            setState(FieldState.FIELD);
                            break;
                        }

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                            string.append(t.getChar());
                            length = string.length();
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case AFTER_NAME:
                    switch (t.getType()) {
                        case COLON:
                            fieldName = takeString();
                            length = -1;
                            setState(FieldState.VALUE);
                            break;

                        case LF:
                            fieldName = takeString();
                            string.reset();
                            fieldValue = "";
                            length = -1;
                            break;

                        case SPACE:
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case VALUE:
                    switch (t.getType()) {
                        case LF:
                            string.reset();
                            fieldValue = "";
                            length = -1;

                            setState(FieldState.FIELD);
                            break;

                        case SPACE:
                        case HTAB:
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                            string.append(t.getByte());
                            length = string.length();
                            setState(FieldState.IN_VALUE);
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case IN_VALUE:
                    switch (t.getType()) {
                        case SPACE:
                        case HTAB:
                            string.append(' ');
                            break;

                        case LF:
                            if (length > 0) {
                                fieldValue = takeString();
                                length = -1;
                                totalHeaderLineLength = -1;
                            }
                            setState(FieldState.FIELD);
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                            string.append(t.getByte());
                            length = string.length();
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                default:
                    throw new IllegalStateException(state.toString());
            }
        }
        return false;
    }

    private void handleField() {
        if (LOG.isDebugEnabled())
            LOG.debug("parsedField:  _fieldName={} _fieldValue={} {}", fieldName, fieldValue, this);

        if (fieldName != null && fieldValue != null)
            handler.parsedField(fieldName, fieldValue);
        fieldName = fieldValue = null;
    }

    protected boolean parseOctetContent(ByteBuffer buffer) {
        if (LOG.isDebugEnabled())
            LOG.debug("parseOctetContent({})", BufferUtils.toDetailString(buffer));

        // Starts With
        if (partialBoundary > 0) {
            int partial = delimiterSearch.startsWith(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), partialBoundary);
            if (partial > 0) {
                if (partial == delimiterSearch.getLength()) {
                    buffer.position(buffer.position() + delimiterSearch.getLength() - partialBoundary);
                    setState(State.DELIMITER);
                    partialBoundary = 0;

                    if (LOG.isDebugEnabled())
                        LOG.debug("Content={}, Last={} {}", BufferUtils.toDetailString(BufferUtils.EMPTY_BUFFER), true, this);

                    return handler.content(BufferUtils.EMPTY_BUFFER, true);
                }

                partialBoundary = partial;
                BufferUtils.clear(buffer);
                return false;
            } else {
                // output up to _partialBoundary of the search pattern
                ByteBuffer content = patternBuffer.slice();
                if (state == State.FIRST_OCTETS) {
                    setState(State.OCTETS);
                    content.position(2);
                }
                content.limit(partialBoundary);
                partialBoundary = 0;

                if (LOG.isDebugEnabled())
                    LOG.debug("Content={}, Last={} {}", BufferUtils.toDetailString(content), false, this);

                if (handler.content(content, false))
                    return true;
            }
        }

        // Contains
        int delimiter = delimiterSearch.match(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        if (delimiter >= 0) {
            ByteBuffer content = buffer.slice();
            content.limit(delimiter - buffer.arrayOffset() - buffer.position());

            buffer.position(delimiter - buffer.arrayOffset() + delimiterSearch.getLength());
            setState(State.DELIMITER);

            if (LOG.isDebugEnabled())
                LOG.debug("Content={}, Last={} {}", BufferUtils.toDetailString(content), true, this);

            return handler.content(content, true);
        }

        // Ends With
        partialBoundary = delimiterSearch.endsWith(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        if (partialBoundary > 0) {
            ByteBuffer content = buffer.slice();
            content.limit(content.limit() - partialBoundary);

            if (LOG.isDebugEnabled())
                LOG.debug("Content={}, Last={} {}", BufferUtils.toDetailString(content), false, this);

            BufferUtils.clear(buffer);
            return handler.content(content, false);
        }

        // There is normal content with no delimiter
        ByteBuffer content = buffer.slice();

        if (LOG.isDebugEnabled())
            LOG.debug("Content={}, Last={} {}", BufferUtils.toDetailString(content), false, this);

        BufferUtils.clear(buffer);
        return handler.content(content, false);
    }

    private void setState(State state) {
        if (LOG.isDebugEnabled())
            LOG.debug("{} --> {}", this.state, state);
        this.state = state;
    }

    private void setState(FieldState state) {
        if (LOG.isDebugEnabled())
            LOG.debug("{}:{} --> {}", this.state, fieldState, state);
        fieldState = state;
    }

    @Override
    public String toString() {
        return String.format("%s{s=%s}", getClass().getSimpleName(), state);
    }

    /*
     * Event Handler interface These methods return true if the caller should process the events so far received (eg return from parseNext and call
     * HttpChannel.handle). If multiple callbacks are called in sequence (eg headerComplete then messageComplete) from the same point in the parsing then it is
     * sufficient for the caller to process the events only once.
     */
    public interface Handler {
        default void startPart() {
        }

        @SuppressWarnings("unused")
        default void parsedField(String name, String value) {
        }

        default boolean headerComplete() {
            return false;
        }

        @SuppressWarnings("unused")
        default boolean content(ByteBuffer item, boolean last) {
            return false;
        }

        default boolean messageComplete() {
            return false;
        }

        default void earlyEOF() {
        }
    }

    @SuppressWarnings("serial")
    private static class IllegalCharacterException extends BadMessageException {
        private IllegalCharacterException(State state, HttpTokens.Token token, ByteBuffer buffer) {
            super(400, String.format("Illegal character %s", token));
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("Illegal character %s in state=%s for buffer %s", token, state, BufferUtils.toDetailString(buffer)));
        }
    }
}
