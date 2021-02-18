package com.fireflysource.net.http.common.v1.decoder;

import com.fireflysource.common.collection.trie.ArrayTernaryTrie;
import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.Utf8StringBuilder;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.codec.PreEncodedHttpField;
import com.fireflysource.net.http.common.exception.BadMessageException;
import com.fireflysource.net.http.common.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static com.fireflysource.net.http.common.model.HttpComplianceSection.MULTIPLE_CONTENT_LENGTHS;
import static com.fireflysource.net.http.common.model.HttpComplianceSection.TRANSFER_ENCODING_WITH_CONTENT_LENGTH;
import static com.fireflysource.net.http.common.model.HttpTokens.EndOfContent;


/**
 * A Parser for 1.0 and 1.1 as defined by RFC7230
 * <p>
 * This parser parses HTTP client and server messages from buffers
 * passed in the {@link #parseNext(ByteBuffer)} method.  The parsed
 * elements of the HTTP message are passed as event calls to the
 * {@link HttpHandler} instance the parser is constructed with.
 * If the passed handler is a {@link RequestHandler} then server side
 * parsing is performed and if it is a {@link ResponseHandler}, then
 * client side parsing is done.
 * </p>
 * <p>
 * The contract of the {@link HttpHandler} API is that if a call returns
 * true then the call to {@link #parseNext(ByteBuffer)} will return as
 * soon as possible also with a true response.  Typically this indicates
 * that the parsing has reached a stage where the caller should process
 * the events accumulated by the handler.    It is the preferred calling
 * style that handling such as calling a servlet to process a request,
 * should be done after a true return from {@link #parseNext(ByteBuffer)}
 * rather than from within the scope of a call like
 * {@link RequestHandler#messageComplete()}
 * </p>
 * <p>
 * For performance, the parse is heavily dependent on the
 * {@link Trie#getBest(ByteBuffer, int, int)} method to look ahead in a
 * single pass for both the structure ( : and CRLF ) and semantic (which
 * header and value) of a header.  Specifically the static {@link HttpHeader#CACHE}
 * is used to lookup common combinations of headers and values
 * (eg. "Connection: close"), or just header names (eg. "Connection:" ).
 * For headers who's value is not known statically (eg. Host, COOKIE) then a
 * per parser dynamic Trie of {@link HttpFields} from previous parsed messages
 * is used to help the parsing of subsequent messages.
 * </p>
 * <p>
 * The parser can work in varying compliance modes:
 * <dl>
 * <dt>RFC7230</dt><dd>(default) Compliance with RFC7230</dd>
 * <dt>RFC2616</dt><dd>Wrapped headers and HTTP/0.9 supported</dd>
 * <dt>LEGACY</dt><dd>(aka STRICT) Adherence to Servlet Specification requirement for
 * exact case of header names, bypassing the header caches, which are case insensitive,
 * otherwise equivalent to RFC2616</dd>
 * </dl>
 *
 * @see <a href="http://tools.ietf.org/html/rfc7230">RFC 7230</a>
 */
public class HttpParser {
    public static final LazyLogger LOG = SystemLogger.create(HttpParser.class);
    @Deprecated
    public final static String STRICT = "com.fireflysource.net.http.common.v1.decoder.HttpParser.STRICT";
    public final static int INITIAL_URI_LENGTH = 256;
    /**
     * Cache of common {@link HttpField}s including: <UL>
     * <LI>Common static combinations such as:<UL>
     * <li>Connection: close
     * <li>Accept-Encoding: gzip
     * <li>Content-Length: 0
     * </ul>
     * <li>Combinations of Content-Type header for common mime types by common charsets
     * <li>Most common headers with null values so that a lookup will at least
     * determine the header name even if the name:value combination is not cached
     * </ul>
     */
    public final static Trie<HttpField> CACHE = new ArrayTrie<>(2048);
    private final static int MAX_CHUNK_LENGTH = Integer.MAX_VALUE / 16 - 16;
    private final static EnumSet<State> IDLE_STATES = EnumSet.of(State.START, State.END, State.CLOSE, State.CLOSED);
    private final static EnumSet<State> COMPLETE_STATES = EnumSet.of(State.END, State.CLOSE, State.CLOSED);
    private static final boolean DEBUG = LOG.isDebugEnabled(); // Cache debug to help branch prediction

    static {
        CACHE.put(new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE));
        CACHE.put(new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE));
        CACHE.put(new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip, deflate"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip, deflate, br"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip,deflate,sdch"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-GB,en-US;q=0.8,en;q=0.6"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-AU,en;q=0.9,it-IT;q=0.8,it;q=0.7,en-GB;q=0.6,en-US;q=0.5"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.3"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT, "*/*"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT, "image/png,image/*;q=0.8,*/*;q=0.5"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"));
        CACHE.put(new HttpField(HttpHeader.ACCEPT_RANGES, HttpHeaderValue.BYTES));
        CACHE.put(new HttpField(HttpHeader.PRAGMA, "no-cache"));
        CACHE.put(new HttpField(HttpHeader.CACHE_CONTROL, "private, no-cache, no-cache=Set-Cookie, proxy-revalidate"));
        CACHE.put(new HttpField(HttpHeader.CACHE_CONTROL, "no-cache"));
        CACHE.put(new HttpField(HttpHeader.CACHE_CONTROL, "max-age=0"));
        CACHE.put(new HttpField(HttpHeader.CONTENT_LENGTH, "0"));
        CACHE.put(new HttpField(HttpHeader.CONTENT_ENCODING, "gzip"));
        CACHE.put(new HttpField(HttpHeader.CONTENT_ENCODING, "deflate"));
        CACHE.put(new HttpField(HttpHeader.TRANSFER_ENCODING, "chunked"));
        CACHE.put(new HttpField(HttpHeader.EXPIRES, "Fri, 01 Jan 1990 00:00:00 GMT"));

        // Add common Content types as fields
        for (String type : new String[]{"text/plain", "text/html", "text/xml", "text/json", "application/json", "application/x-www-form-urlencoded"}) {
            HttpField field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type);
            CACHE.put(field);

            for (String charset : new String[]{"utf-8", "iso-8859-1"}) {
                CACHE.put(new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + ";charset=" + charset));
                CACHE.put(new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + "; charset=" + charset));
                CACHE.put(new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + ";charset=" + charset.toUpperCase(Locale.ENGLISH)));
                CACHE.put(new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + "; charset=" + charset.toUpperCase(Locale.ENGLISH)));
            }
        }

        // Add headers with null values so HttpParser can avoid looking up name again for unknown values
        for (HttpHeader h : HttpHeader.values())
            if (!CACHE.put(new HttpField(h, (String) null)))
                throw new IllegalStateException("CACHE FULL");
    }

    private final HttpHandler handler;
    private final RequestHandler requestHandler;
    private final ResponseHandler responseHandler;
    private final ComplianceHandler complianceHandler;
    private final int maxHeaderBytes;
    private final HttpCompliance compliance;
    private final EnumSet<HttpComplianceSection> complianceSections;
    private final StringBuilder string = new StringBuilder();
    private HttpField field;
    private HttpHeader header;
    private String headerString;
    private String valueString;
    private int responseStatus;
    private int headerBytes;
    private boolean host;
    private boolean headerComplete;

    private State state = State.START;
    private FieldState fieldState = FieldState.FIELD;
    private boolean eof;
    private HttpMethod method;
    private String methodString;
    private HttpVersion version;
    private final Utf8StringBuilder uri = new Utf8StringBuilder(INITIAL_URI_LENGTH); // Tune?
    private EndOfContent endOfContent;
    private boolean hasContentLength;
    private long contentLength = -1;
    private long contentPosition;
    private int chunkLength;
    private int chunkPosition;
    private boolean headResponse;
    private boolean cr;
    private ByteBuffer contentChunk;
    private Trie<HttpField> fieldCache;

    private int length;

    public HttpParser(RequestHandler handler) {
        this(handler, -1, compliance());
    }

    public HttpParser(ResponseHandler handler) {
        this(handler, -1, compliance());
    }

    public HttpParser(RequestHandler handler, int maxHeaderBytes) {
        this(handler, maxHeaderBytes, compliance());
    }


    public HttpParser(ResponseHandler handler, int maxHeaderBytes) {
        this(handler, maxHeaderBytes, compliance());
    }


    @Deprecated
    public HttpParser(RequestHandler handler, int maxHeaderBytes, boolean strict) {
        this(handler, maxHeaderBytes, strict ? HttpCompliance.LEGACY : compliance());
    }


    @Deprecated
    public HttpParser(ResponseHandler handler, int maxHeaderBytes, boolean strict) {
        this(handler, maxHeaderBytes, strict ? HttpCompliance.LEGACY : compliance());
    }


    public HttpParser(RequestHandler handler, HttpCompliance compliance) {
        this(handler, -1, compliance);
    }


    public HttpParser(RequestHandler handler, int maxHeaderBytes, HttpCompliance compliance) {
        this(handler, null, maxHeaderBytes, compliance == null ? compliance() : compliance);
    }


    public HttpParser(ResponseHandler handler, int maxHeaderBytes, HttpCompliance compliance) {
        this(null, handler, maxHeaderBytes, compliance == null ? compliance() : compliance);
    }


    private HttpParser(RequestHandler requestHandler, ResponseHandler responseHandler, int maxHeaderBytes, HttpCompliance compliance) {
        handler = requestHandler != null ? requestHandler : responseHandler;
        this.requestHandler = requestHandler;
        this.responseHandler = responseHandler;
        this.maxHeaderBytes = maxHeaderBytes;
        this.compliance = compliance;
        complianceSections = compliance.sections();
        complianceHandler = (ComplianceHandler) (handler instanceof ComplianceHandler ? handler : null);
    }

    private static HttpCompliance compliance() {
        boolean strict = Boolean.getBoolean(STRICT);
        if (strict) {
            LOG.warn("Deprecated property used: " + STRICT);
            return HttpCompliance.LEGACY;
        }
        return HttpCompliance.RFC7230;
    }

    public HttpHandler getHandler() {
        return handler;
    }

    /**
     * Check RFC compliance violation
     *
     * @param violation The compliance section violation
     * @return True if the current compliance level is set so as to Not allow this violation
     */
    protected boolean complianceViolation(HttpComplianceSection violation) {
        return complianceViolation(violation, null);
    }

    /**
     * Check RFC compliance violation
     *
     * @param violation The compliance section violation
     * @param reason    The reason for the violation
     * @return True if the current compliance level is set so as to Not allow this violation
     */
    protected boolean complianceViolation(HttpComplianceSection violation, String reason) {
        if (complianceSections.contains(violation))
            return true;
        if (reason == null)
            reason = violation.getDescription();
        if (complianceHandler != null)
            complianceHandler.onComplianceViolation(compliance, violation, reason);

        return false;
    }

    protected void handleViolation(HttpComplianceSection section, String reason) {
        if (complianceHandler != null)
            complianceHandler.onComplianceViolation(compliance, section, reason);
    }

    protected String caseInsensitiveHeader(String orig, String normative) {
        if (complianceSections.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE))
            return normative;
        if (!orig.equals(normative))
            handleViolation(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, orig);
        return orig;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getContentRead() {
        return contentPosition;
    }

    /**
     * Set if a HEAD response is expected
     *
     * @param head true if head response is expected
     */
    public void setHeadResponse(boolean head) {
        headResponse = head;
    }

    protected void setResponseStatus(int status) {
        responseStatus = status;
    }

    public State getState() {
        return state;
    }

    protected void setState(State state) {
        if (DEBUG)
            LOG.debug("{} --> {}", this.state, state);
        this.state = state;
    }

    protected void setState(FieldState state) {
        if (DEBUG)
            LOG.debug("{}:{} --> {}", this.state, field != null ? field : headerString != null ? headerString : string, state);
        fieldState = state;
    }

    public boolean inContentState() {
        return state.ordinal() >= State.CONTENT.ordinal() && state.ordinal() < State.END.ordinal();
    }


    public boolean inHeaderState() {
        return state.ordinal() < State.CONTENT.ordinal();
    }


    public boolean isChunking() {
        return endOfContent == EndOfContent.CHUNKED_CONTENT;
    }


    public boolean isStart() {
        return isState(State.START);
    }


    public boolean isClose() {
        return isState(State.CLOSE);
    }


    public boolean isClosed() {
        return isState(State.CLOSED);
    }


    public boolean isIdle() {
        return IDLE_STATES.contains(state);
    }


    public boolean isComplete() {
        return COMPLETE_STATES.contains(state);
    }


    public boolean isState(State state) {
        return this.state == state;
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
                if (buffer.hasRemaining()) {
                    // Don't count the CRs and LFs of the chunked encoding.
                    if (maxHeaderBytes > 0 && (state == State.HEADER || state == State.TRAILER))
                        headerBytes++;
                    return next(buffer);
                }

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


    /* Quick lookahead for the start state looking for a request method or a HTTP version,
     * otherwise skip white space until something else to parse.
     */
    private boolean quickStart(ByteBuffer buffer) {
        if (requestHandler != null) {
            method = HttpMethod.lookAheadGet(buffer);
            if (method != null) {
                methodString = method.getValue();
                buffer.position(buffer.position() + methodString.length() + 1);

                setState(State.SPACE1);
                return false;
            }
        } else if (responseHandler != null) {
            version = HttpVersion.lookAheadGet(buffer);
            if (version != null) {
                buffer.position(buffer.position() + version.getValue().length() + 1);
                setState(State.SPACE1);
                return false;
            }
        }

        // Quick start look
        while (state == State.START && buffer.hasRemaining()) {
            HttpTokens.Token t = next(buffer);
            if (t == null)
                break;

            switch (t.getType()) {
                case ALPHA:
                case DIGIT:
                case TCHAR:
                case VCHAR: {
                    string.setLength(0);
                    string.append(t.getChar());
                    setState(requestHandler != null ? State.METHOD : State.RESPONSE_VERSION);
                    return false;
                }
                case OTEXT:
                case SPACE:
                case HTAB:
                    throw new IllegalCharacterException(state, t, buffer);

                default:
                    break;
            }

            // count this white space as a header byte to avoid DOS
            if (maxHeaderBytes > 0 && ++headerBytes > maxHeaderBytes) {
                LOG.warn("padding is too large >" + maxHeaderBytes);
                throw new BadMessageException(HttpStatus.BAD_REQUEST_400);
            }
        }
        return false;
    }


    private void setString(String s) {
        string.setLength(0);
        string.append(s);
        length = s.length();
    }


    private String takeString() {
        string.setLength(length);
        String s = string.toString();
        string.setLength(0);
        length = -1;
        return s;
    }


    private boolean handleHeaderContentMessage() {
        boolean handle_header = handler.headerComplete();
        headerComplete = true;
        boolean handle_content = handler.contentComplete();
        boolean handle_message = handler.messageComplete();
        return handle_header || handle_content || handle_message;
    }


    private boolean handleContentMessage() {
        boolean handle_content = handler.contentComplete();
        boolean handle_message = handler.messageComplete();
        return handle_content || handle_message;
    }


    /* Parse a request or response line
     */
    private boolean parseLine(ByteBuffer buffer) {
        boolean handle = false;

        // Process headers
        while (state.ordinal() < State.HEADER.ordinal() && buffer.hasRemaining() && !handle) {
            // process each character
            HttpTokens.Token t = next(buffer);
            if (t == null)
                break;

            if (maxHeaderBytes > 0 && ++headerBytes > maxHeaderBytes) {
                if (state == State.URI) {
                    LOG.warn("URI is too large >" + maxHeaderBytes);
                    throw new BadMessageException(HttpStatus.URI_TOO_LONG_414);
                } else {
                    if (requestHandler != null)
                        LOG.warn("request is too large >" + maxHeaderBytes);
                    else
                        LOG.warn("response is too large >" + maxHeaderBytes);
                    throw new BadMessageException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE_431);
                }
            }

            switch (state) {
                case METHOD:
                    switch (t.getType()) {
                        case SPACE:
                            length = string.length();
                            methodString = takeString();

                            if (complianceSections.contains(HttpComplianceSection.METHOD_CASE_SENSITIVE)) {
                                HttpMethod method = HttpMethod.CACHE.get(methodString);
                                if (method != null)
                                    methodString = method.getValue();
                            } else {
                                HttpMethod method = HttpMethod.INSENSITIVE_CACHE.get(methodString);

                                if (method != null) {
                                    if (!method.getValue().equals(methodString))
                                        handleViolation(HttpComplianceSection.METHOD_CASE_SENSITIVE, methodString);
                                    methodString = method.getValue();
                                }
                            }

                            setState(State.SPACE1);
                            break;

                        case LF:
                            throw new BadMessageException("No URI");

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                            string.append(t.getChar());
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case RESPONSE_VERSION:
                    switch (t.getType()) {
                        case SPACE:
                            length = string.length();
                            String version = takeString();
                            this.version = HttpVersion.CACHE.get(version);
                            checkVersion();
                            setState(State.SPACE1);
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            string.append(t.getChar());
                            break;
                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case SPACE1:
                    switch (t.getType()) {
                        case SPACE:
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            if (responseHandler != null) {
                                if (t.getType() != HttpTokens.Type.DIGIT) {
                                    throw new IllegalCharacterException(state, t, buffer);
                                }
                                setState(State.STATUS);
                                setResponseStatus(t.getByte() - '0');
                            } else {
                                uri.reset();
                                setState(State.URI);
                                // quick scan for space or EoBuffer
                                if (buffer.hasArray()) {
                                    byte[] array = buffer.array();
                                    int p = buffer.arrayOffset() + buffer.position();
                                    int l = buffer.arrayOffset() + buffer.limit();
                                    int i = p;
                                    while (i < l && array[i] > HttpTokens.SPACE) {
                                        i++;
                                    }
                                    int len = i - p;
                                    headerBytes += len;

                                    if (maxHeaderBytes > 0 && ++headerBytes > maxHeaderBytes) {
                                        LOG.warn("URI is too large >" + maxHeaderBytes);
                                        throw new BadMessageException(HttpStatus.URI_TOO_LONG_414);
                                    }
                                    uri.append(array, p - 1, len + 1);
                                    buffer.position(i - buffer.arrayOffset());
                                } else {
                                    uri.append(t.getByte());
                                }
                            }
                            break;

                        default:
                            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, requestHandler != null ? "No URI" : "No Status");
                    }
                    break;

                case STATUS:
                    switch (t.getType()) {
                        case SPACE:
                            setState(State.SPACE2);
                            break;

                        case DIGIT:
                            responseStatus = responseStatus * 10 + (t.getByte() - '0');
                            if (responseStatus >= 1000) {
                                throw new BadMessageException("Bad status");
                            }
                            break;

                        case LF:
                            setState(State.HEADER);
                            handle |= responseHandler.startResponse(version, responseStatus, null);
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case URI:
                    switch (t.getType()) {
                        case SPACE:
                            setState(State.SPACE2);
                            break;

                        case LF:
                            // HTTP/0.9
                            if (complianceViolation(HttpComplianceSection.NO_HTTP_0_9, "No request version")) {
                                throw new BadMessageException("HTTP/0.9 not supported");
                            }
                            handle = requestHandler.startRequest(methodString, uri.toString(), HttpVersion.HTTP_0_9);
                            setState(State.END);
                            BufferUtils.clear(buffer);
                            handle |= handleHeaderContentMessage();
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT:
                            uri.append(t.getByte());
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case SPACE2:
                    switch (t.getType()) {
                        case SPACE:
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            string.setLength(0);
                            string.append(t.getChar());
                            if (responseHandler != null) {
                                length = 1;
                                setState(State.REASON);
                            } else {
                                setState(State.REQUEST_VERSION);

                                // try quick look ahead for HTTP Version
                                HttpVersion version;
                                if (buffer.position() > 0 && buffer.hasArray())
                                    version = HttpVersion.lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position() - 1, buffer.arrayOffset() + buffer.limit());
                                else
                                    version = HttpVersion.CACHE.getBest(buffer, 0, buffer.remaining());

                                if (version != null) {
                                    int pos = buffer.position() + version.getValue().length() - 1;
                                    if (pos < buffer.limit()) {
                                        byte n = buffer.get(pos);
                                        if (n == HttpTokens.CARRIAGE_RETURN) {
                                            cr = true;
                                            this.version = version;
                                            checkVersion();
                                            string.setLength(0);
                                            buffer.position(pos + 1);
                                        } else if (n == HttpTokens.LINE_FEED) {
                                            this.version = version;
                                            checkVersion();
                                            string.setLength(0);
                                            buffer.position(pos);
                                        }
                                    }
                                }
                            }
                            break;

                        case LF:
                            if (responseHandler != null) {
                                setState(State.HEADER);
                                handle |= responseHandler.startResponse(version, responseStatus, null);
                            } else {
                                // HTTP/0.9
                                if (complianceViolation(HttpComplianceSection.NO_HTTP_0_9, "No request version")) {
                                    throw new BadMessageException("HTTP/0.9 not supported");
                                }
                                handle = requestHandler.startRequest(methodString, uri.toString(), HttpVersion.HTTP_0_9);
                                setState(State.END);
                                BufferUtils.clear(buffer);
                                handle |= handleHeaderContentMessage();
                            }
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case REQUEST_VERSION:
                    switch (t.getType()) {
                        case LF:
                            if (version == null) {
                                length = string.length();
                                version = HttpVersion.CACHE.get(takeString());
                            }
                            checkVersion();

                            // Should we try to cache header fields?
                            if (fieldCache == null && version.getVersion() >= HttpVersion.HTTP_1_1.getVersion() && handler.getHeaderCacheSize() > 0) {
                                int header_cache = handler.getHeaderCacheSize();
                                fieldCache = new ArrayTernaryTrie<>(header_cache);
                            }

                            setState(State.HEADER);

                            handle |= requestHandler.startRequest(methodString, uri.toString(), version);
                            continue;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                            string.append(t.getChar());
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case REASON:
                    switch (t.getType()) {
                        case LF:
                            String reason = takeString();
                            setState(State.HEADER);
                            handle |= responseHandler.startResponse(version, responseStatus, reason);
                            continue;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT: // TODO should this be UTF8
                            string.append(t.getChar());
                            length = string.length();
                            break;

                        case SPACE:
                        case HTAB:
                            string.append(t.getChar());
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                default:
                    throw new IllegalStateException(state.toString());
            }
        }

        return handle;
    }

    private void checkVersion() {
        if (version == null) {
            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Unknown Version");
        }
        if (version.getVersion() < 10 || version.getVersion() > 20) {
            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad Version");
        }
    }

    private void parsedHeader() {
        // handler last header if any.  Delayed to here just in case there was a continuation line (above)
        if (headerString != null || valueString != null) {
            // Handle known headers
            if (header != null) {
                boolean add_to_connection_trie = false;
                switch (header) {
                    case CONTENT_LENGTH:
                        if (hasContentLength) {
                            if (complianceViolation(MULTIPLE_CONTENT_LENGTHS)) {
                                throw new BadMessageException(HttpStatus.BAD_REQUEST_400, MULTIPLE_CONTENT_LENGTHS.getDescription());
                            }
                            if (convertContentLength(valueString) != contentLength) {
                                throw new BadMessageException(HttpStatus.BAD_REQUEST_400, MULTIPLE_CONTENT_LENGTHS.getDescription());
                            }
                        }
                        hasContentLength = true;

                        if (endOfContent == EndOfContent.CHUNKED_CONTENT && complianceViolation(TRANSFER_ENCODING_WITH_CONTENT_LENGTH)) {
                            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad Content-Length");
                        }
                        if (endOfContent != EndOfContent.CHUNKED_CONTENT) {
                            contentLength = convertContentLength(valueString);
                            if (contentLength <= 0) {
                                endOfContent = EndOfContent.NO_CONTENT;
                            } else {
                                endOfContent = EndOfContent.CONTENT_LENGTH;
                            }
                        }
                        break;

                    case TRANSFER_ENCODING:
                        if (hasContentLength && complianceViolation(TRANSFER_ENCODING_WITH_CONTENT_LENGTH)) {
                            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Transfer-Encoding and Content-Length");
                        }
                        if (HttpHeaderValue.CHUNKED.is(valueString)) {
                            endOfContent = EndOfContent.CHUNKED_CONTENT;
                            contentLength = -1;
                        } else {
                            List<String> values = new QuotedCSV(valueString).getValues();
                            if (values.size() > 0 && HttpHeaderValue.CHUNKED.is(values.get(values.size() - 1))) {
                                endOfContent = EndOfContent.CHUNKED_CONTENT;
                                contentLength = -1;
                            } else if (values.stream().anyMatch(HttpHeaderValue.CHUNKED::is)) {
                                throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad chunking");
                            }
                        }
                        break;

                    case HOST:
                        host = true;
                        if (!(field instanceof HostPortHttpField) && valueString != null && !valueString.isEmpty()) {
                            field = new HostPortHttpField(header,
                                    complianceSections.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE) ? header.getValue() : headerString,
                                    valueString);
                            add_to_connection_trie = fieldCache != null;
                        }
                        break;

                    case CONNECTION:
                        // Don't cache headers if not persistent
                        if (HttpHeaderValue.CLOSE.is(valueString) || new QuotedCSV(valueString).getValues().stream().anyMatch(HttpHeaderValue.CLOSE::is)) {
                            fieldCache = null;
                        }
                        break;

                    case AUTHORIZATION:
                    case ACCEPT:
                    case ACCEPT_CHARSET:
                    case ACCEPT_ENCODING:
                    case ACCEPT_LANGUAGE:
                    case COOKIE:
                    case CACHE_CONTROL:
                    case USER_AGENT:
                        add_to_connection_trie = fieldCache != null && field == null;
                        break;

                    default:
                        break;

                }

                if (add_to_connection_trie && !fieldCache.isFull() && header != null && valueString != null) {
                    if (field == null) {
                        field = new HttpField(header, caseInsensitiveHeader(headerString, header.getValue()), valueString);
                    }
                    fieldCache.put(field);
                }
            }
            handler.parsedHeader(field != null ? field : new HttpField(header, headerString, valueString));
        }

        headerString = valueString = null;
        header = null;
        field = null;
    }

    private void parsedTrailer() {
        // handler last header if any.  Delayed to here just in case there was a continuation line (above)
        if (headerString != null || valueString != null) {
            handler.parsedTrailer(field != null ? field : new HttpField(header, headerString, valueString));
        }
        headerString = valueString = null;
        header = null;
        field = null;
    }

    private long convertContentLength(String valueString) {
        try {
            return Long.parseLong(valueString);
        } catch (NumberFormatException e) {
            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Invalid Content-Length Value", e);
        }
    }


    /*
     * Parse the message headers and return true if the handler has signalled for a return
     */
    protected boolean parseFields(ByteBuffer buffer) {
        // Process headers
        while ((state == State.HEADER || state == State.TRAILER) && buffer.hasRemaining()) {
            // process each character
            HttpTokens.Token t = next(buffer);
            if (t == null) {
                break;
            }
            if (maxHeaderBytes > 0 && ++headerBytes > maxHeaderBytes) {
                boolean header = state == State.HEADER;
                LOG.warn("{} is too large {}>{}", header ? "Header" : "Trailer", headerBytes, maxHeaderBytes);
                throw new BadMessageException(header ?
                        HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE_431 :
                        HttpStatus.PAYLOAD_TOO_LARGE_413);
            }

            switch (fieldState) {
                case FIELD:
                    switch (t.getType()) {
                        case COLON:
                        case SPACE:
                        case HTAB: {
                            if (complianceViolation(HttpComplianceSection.NO_FIELD_FOLDING, headerString)) {
                                throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Header Folding");
                            }
                            // header value without name - continuation?
                            if (valueString == null || valueString.isEmpty()) {
                                string.setLength(0);
                                length = 0;
                            } else {
                                setString(valueString);
                                string.append(' ');
                                length++;
                                valueString = null;
                            }
                            setState(FieldState.VALUE);
                            break;
                        }

                        case LF: {
                            // process previous header
                            if (state == State.HEADER) {
                                parsedHeader();
                            } else {
                                parsedTrailer();
                            }
                            contentPosition = 0;

                            // End of headers or trailers?
                            if (state == State.TRAILER) {
                                setState(State.END);
                                return handler.messageComplete();
                            }

                            // Was there a required host header?
                            if (!host && version == HttpVersion.HTTP_1_1 && requestHandler != null) {
                                throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "No Host");
                            }

                            // is it a response that cannot have a body?
                            if (responseHandler != null && // response
                                    (responseStatus == 304 || // not-modified response
                                            responseStatus == 204 || // no-content response
                                            responseStatus < 200)) { // 1xx response
                                endOfContent = EndOfContent.NO_CONTENT; // ignore any other headers set
                                // else if we don't know framing
                            } else if (endOfContent == EndOfContent.UNKNOWN_CONTENT) {
                                if (responseStatus == 0  // request
                                        || responseStatus == 304 // not-modified response
                                        || responseStatus == 204 // no-content response
                                        || responseStatus < 200) { // 1xx response
                                    endOfContent = EndOfContent.NO_CONTENT;
                                } else {
                                    endOfContent = EndOfContent.EOF_CONTENT;
                                }
                            }

                            // How is the message ended?
                            switch (endOfContent) {
                                case EOF_CONTENT: {
                                    setState(State.EOF_CONTENT);
                                    boolean handle = handler.headerComplete();
                                    headerComplete = true;
                                    return handle;
                                }
                                case CHUNKED_CONTENT: {
                                    setState(State.CHUNKED_CONTENT);
                                    boolean handle = handler.headerComplete();
                                    headerComplete = true;
                                    return handle;
                                }
                                case NO_CONTENT: {
                                    setState(State.END);
                                    return handleHeaderContentMessage();
                                }
                                default: {
                                    setState(State.CONTENT);
                                    boolean handle = handler.headerComplete();
                                    headerComplete = true;
                                    return handle;
                                }
                            }
                        }

                        case ALPHA:
                        case DIGIT:
                        case TCHAR: {
                            // process previous header
                            if (state == State.HEADER) {
                                parsedHeader();
                            } else {
                                parsedTrailer();
                            }
                            // handle new header
                            if (buffer.hasRemaining()) {
                                // Try a look ahead for the known header name and value.
                                HttpField cached_field = fieldCache == null ? null : fieldCache.getBest(buffer, -1, buffer.remaining());
                                if (cached_field == null) {
                                    cached_field = CACHE.getBest(buffer, -1, buffer.remaining());
                                }
                                if (cached_field != null) {
                                    String n = cached_field.getName();
                                    String v = cached_field.getValue();

                                    if (!complianceSections.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE)) {
                                        // Have to get the fields exactly from the buffer to match case
                                        String en = BufferUtils.toString(buffer, buffer.position() - 1, n.length(), StandardCharsets.US_ASCII);
                                        if (!n.equals(en)) {
                                            handleViolation(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, en);
                                            n = en;
                                            cached_field = new HttpField(cached_field.getHeader(), n, v);
                                        }
                                    }

                                    if (v != null && !complianceSections.contains(HttpComplianceSection.CASE_INSENSITIVE_FIELD_VALUE_CACHE)) {
                                        String ev = BufferUtils.toString(buffer, buffer.position() + n.length() + 1, v.length(), StandardCharsets.ISO_8859_1);
                                        if (!v.equals(ev)) {
                                            handleViolation(HttpComplianceSection.CASE_INSENSITIVE_FIELD_VALUE_CACHE, ev + "!=" + v);
                                            v = ev;
                                            cached_field = new HttpField(cached_field.getHeader(), n, v);
                                        }
                                    }

                                    header = cached_field.getHeader();
                                    headerString = n;

                                    if (v == null) {
                                        // Header only
                                        setState(FieldState.VALUE);
                                        string.setLength(0);
                                        length = 0;
                                        buffer.position(buffer.position() + n.length() + 1);
                                        break;
                                    }

                                    // Header and value
                                    int pos = buffer.position() + n.length() + v.length() + 1;
                                    byte peek = buffer.get(pos);
                                    if (peek == HttpTokens.CARRIAGE_RETURN || peek == HttpTokens.LINE_FEED) {
                                        field = cached_field;
                                        valueString = v;
                                        setState(FieldState.IN_VALUE);

                                        if (peek == HttpTokens.CARRIAGE_RETURN) {
                                            cr = true;
                                            buffer.position(pos + 1);
                                        } else {
                                            buffer.position(pos);
                                        }
                                        break;
                                    }
                                    setState(FieldState.IN_VALUE);
                                    setString(v);
                                    buffer.position(pos);
                                    break;
                                }
                            }

                            // New header
                            setState(FieldState.IN_NAME);
                            string.setLength(0);
                            string.append(t.getChar());
                            length = 1;
                        }
                        break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case IN_NAME:
                    switch (t.getType()) {
                        case SPACE:
                        case HTAB:
                            //Ignore trailing whitespaces ?
                            if (!complianceViolation(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME, null)) {
                                headerString = takeString();
                                header = HttpHeader.CACHE.get(headerString);
                                length = -1;
                                setState(FieldState.WS_AFTER_NAME);
                                break;
                            }
                            throw new IllegalCharacterException(state, t, buffer);

                        case COLON:
                            headerString = takeString();
                            header = HttpHeader.CACHE.get(headerString);
                            length = -1;
                            setState(FieldState.VALUE);
                            break;

                        case LF:
                            headerString = takeString();
                            header = HttpHeader.CACHE.get(headerString);
                            string.setLength(0);
                            valueString = "";
                            length = -1;

                            if (!complianceViolation(HttpComplianceSection.FIELD_COLON, headerString)) {
                                setState(FieldState.FIELD);
                                break;
                            }
                            throw new IllegalCharacterException(state, t, buffer);

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

                case WS_AFTER_NAME:

                    switch (t.getType()) {
                        case SPACE:
                        case HTAB:
                            break;

                        case COLON:
                            setState(FieldState.VALUE);
                            break;

                        case LF:
                            if (!complianceViolation(HttpComplianceSection.FIELD_COLON, headerString)) {
                                setState(FieldState.FIELD);
                                break;
                            }
                            throw new IllegalCharacterException(state, t, buffer);

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case VALUE:
                    switch (t.getType()) {
                        case LF:
                            string.setLength(0);
                            valueString = "";
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
                        case OTEXT: // TODO review? should this be a utf8 string?
                            string.append(t.getChar());
                            length = string.length();
                            setState(FieldState.IN_VALUE);
                            break;

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;

                case IN_VALUE:
                    switch (t.getType()) {
                        case LF:
                            if (length > 0) {
                                valueString = takeString();
                                length = -1;
                            }
                            setState(FieldState.FIELD);
                            break;

                        case SPACE:
                        case HTAB:
                            string.append(t.getChar());
                            break;

                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case COLON:
                        case OTEXT: // TODO review? should this be a utf8 string?
                            string.append(t.getChar());
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


    /**
     * Parse until next Event.
     *
     * @param buffer the buffer to parse
     * @return True if an {@link RequestHandler} method was called and it returned true;
     */
    public boolean parseNext(ByteBuffer buffer) {
        if (DEBUG)
            LOG.debug("parseNext s={} {}", state, BufferUtils.toDetailString(buffer));
        try {
            // Start a request/response
            if (state == State.START) {
                version = null;
                method = null;
                methodString = null;
                endOfContent = EndOfContent.UNKNOWN_CONTENT;
                header = null;
                if (quickStart(buffer)) {
                    return true;
                }
            }

            // Request/response line
            if (state.ordinal() >= State.START.ordinal() && state.ordinal() < State.HEADER.ordinal()) {
                if (parseLine(buffer)) {
                    return true;
                }
            }

            // parse headers
            if (state == State.HEADER) {
                if (parseFields(buffer)) {
                    return true;
                }
            }

            // parse content
            if (state.ordinal() >= State.CONTENT.ordinal() && state.ordinal() < State.TRAILER.ordinal()) {
                // Handle HEAD response
                if (responseStatus > 0 && headResponse) {
                    setState(State.END);
                    return handleContentMessage();
                } else {
                    if (parseContent(buffer)) {
                        return true;
                    }
                }
            }

            // parse headers
            if (state == State.TRAILER) {
                if (parseFields(buffer)) {
                    return true;
                }
            }

            // handle end states
            if (state == State.END) {
                // eat white space
                while (buffer.remaining() > 0 && buffer.get(buffer.position()) <= HttpTokens.SPACE) {
                    buffer.get();
                }
            } else if (isClose() || isClosed()) {
                BufferUtils.clear(buffer);
            }

            // Handle EOF
            if (eof && !buffer.hasRemaining()) {
                switch (state) {
                    case CLOSED:
                        break;

                    case START:
                        setState(State.CLOSED);
                        handler.earlyEOF();
                        break;

                    case END:
                    case CLOSE:
                        setState(State.CLOSED);
                        break;

                    case EOF_CONTENT:
                    case TRAILER:
                        if (fieldState == FieldState.FIELD) {
                            // Be forgiving of missing last CRLF
                            setState(State.CLOSED);
                            return handleContentMessage();
                        }
                        setState(State.CLOSED);
                        handler.earlyEOF();
                        break;

                    case CONTENT:
                    case CHUNKED_CONTENT:
                    case CHUNK_SIZE:
                    case CHUNK_PARAMS:
                    case CHUNK:
                        setState(State.CLOSED);
                        handler.earlyEOF();
                        break;

                    default:
                        if (DEBUG)
                            LOG.debug("{} EOF in {}", this, state);
                        setState(State.CLOSED);
                        handler.badMessage(new BadMessageException(HttpStatus.BAD_REQUEST_400));
                        break;
                }
            }
        } catch (BadMessageException x) {
            BufferUtils.clear(buffer);
            badMessage(x);
        } catch (Throwable x) {
            BufferUtils.clear(buffer);
            badMessage(new BadMessageException(HttpStatus.BAD_REQUEST_400, requestHandler != null ? "Bad Request" : "Bad Response", x));
        }
        return false;
    }

    protected void badMessage(BadMessageException x) {
        if (DEBUG)
            LOG.debug("Parse exception: " + this + " for " + handler, x);
        setState(State.CLOSE);
        if (headerComplete)
            handler.earlyEOF();
        else
            handler.badMessage(x);
    }

    protected boolean parseContent(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        if (remaining == 0 && state == State.CONTENT) {
            long content = contentLength - contentPosition;
            if (content == 0) {
                setState(State.END);
                return handleContentMessage();
            }
        }

        // Handle _content
        while (state.ordinal() < State.TRAILER.ordinal() && remaining > 0) {
            switch (state) {
                case EOF_CONTENT:
                    contentChunk = buffer.duplicate();
                    contentPosition += remaining;
                    buffer.position(buffer.position() + remaining);
                    if (handler.content(contentChunk)) {
                        return true;
                    }
                    break;

                case CONTENT: {
                    long content = contentLength - contentPosition;
                    if (content == 0) {
                        setState(State.END);
                        return handleContentMessage();
                    } else {
                        contentChunk = buffer.duplicate();

                        // limit content by expected size
                        if (remaining > content) {
                            // We can cast remaining to an int as we know that it is smaller than
                            // or equal to length which is already an int.
                            contentChunk.limit(contentChunk.position() + (int) content);
                        }

                        contentPosition += contentChunk.remaining();
                        buffer.position(buffer.position() + contentChunk.remaining());

                        if (handler.content(contentChunk)) {
                            return true;
                        }
                        if (contentPosition == contentLength) {
                            setState(State.END);
                            return handleContentMessage();
                        }
                    }
                    break;
                }

                case CHUNKED_CONTENT: {
                    HttpTokens.Token t = next(buffer);
                    if (t == null)
                        break;
                    switch (t.getType()) {
                        case LF:
                            break;

                        case DIGIT:
                            chunkLength = t.getHexDigit();
                            chunkPosition = 0;
                            setState(State.CHUNK_SIZE);
                            break;

                        case ALPHA:
                            if (t.isHexDigit()) {
                                chunkLength = t.getHexDigit();
                                chunkPosition = 0;
                                setState(State.CHUNK_SIZE);
                                break;
                            }
                            throw new IllegalCharacterException(state, t, buffer);

                        default:
                            throw new IllegalCharacterException(state, t, buffer);
                    }
                    break;
                }

                case CHUNK_SIZE: {
                    HttpTokens.Token t = next(buffer);
                    if (t == null) {
                        break;
                    }
                    switch (t.getType()) {
                        case LF:
                            if (chunkLength == 0) {
                                setState(State.TRAILER);
                                if (handler.contentComplete())
                                    return true;
                            } else {
                                setState(State.CHUNK);
                            }
                            break;

                        case SPACE:
                            setState(State.CHUNK_PARAMS);
                            break;

                        default:
                            if (t.isHexDigit()) {
                                if (chunkLength > MAX_CHUNK_LENGTH) {
                                    throw new BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413);
                                }
                                chunkLength = chunkLength * 16 + t.getHexDigit();
                            } else {
                                setState(State.CHUNK_PARAMS);
                            }
                    }
                    break;
                }

                case CHUNK_PARAMS: {
                    HttpTokens.Token t = next(buffer);
                    if (t == null) {
                        break;
                    }
                    if (t.getType() == HttpTokens.Type.LF) {
                        if (chunkLength == 0) {
                            setState(State.TRAILER);
                            if (handler.contentComplete()) {
                                return true;
                            }
                        } else {
                            setState(State.CHUNK);
                        }
                    }
                    break;
                }

                case CHUNK: {
                    int chunk = chunkLength - chunkPosition;
                    if (chunk == 0) {
                        setState(State.CHUNKED_CONTENT);
                    } else {
                        contentChunk = buffer.duplicate();

                        if (remaining > chunk) {
                            contentChunk.limit(contentChunk.position() + chunk);
                        }
                        chunk = contentChunk.remaining();

                        contentPosition += chunk;
                        chunkPosition += chunk;
                        buffer.position(buffer.position() + chunk);
                        if (handler.content(contentChunk)) {
                            return true;
                        }
                    }
                    break;
                }

                case CLOSED: {
                    BufferUtils.clear(buffer);
                    return false;
                }

                default:
                    break;

            }

            remaining = buffer.remaining();
        }
        return false;
    }


    public boolean isAtEOF() {
        return eof;
    }


    /**
     * Signal that the associated data source is at EOF
     */
    public void atEOF() {
        if (DEBUG)
            LOG.debug("atEOF {}", this);
        eof = true;
    }


    /**
     * Request that the associated data source be closed
     */
    public void close() {
        if (DEBUG)
            LOG.debug("close {}", this);
        setState(State.CLOSE);
    }


    public void reset() {
        if (DEBUG) {
            LOG.debug("reset {}", this);
        }
        // reset state
        if (state == State.CLOSE || state == State.CLOSED) {
            return;
        }
        setState(State.START);
        endOfContent = EndOfContent.UNKNOWN_CONTENT;
        contentLength = -1;
        hasContentLength = false;
        contentPosition = 0;
        responseStatus = 0;
        contentChunk = null;
        headerBytes = 0;
        host = false;
        headerComplete = false;
    }

    public Trie<HttpField> getFieldCache() {
        return fieldCache;
    }

    @Override
    public String toString() {
        return String.format("%s{s=%s,%d of %d}",
                getClass().getSimpleName(),
                state,
                contentPosition,
                contentLength);
    }


    // States
    public enum FieldState {
        FIELD,
        IN_NAME,
        VALUE,
        IN_VALUE,
        WS_AFTER_NAME,
    }


    // States
    public enum State {
        START,
        METHOD,
        RESPONSE_VERSION,
        SPACE1,
        STATUS,
        URI,
        SPACE2,
        REQUEST_VERSION,
        REASON,
        PROXY,
        HEADER,
        CONTENT,
        EOF_CONTENT,
        CHUNKED_CONTENT,
        CHUNK_SIZE,
        CHUNK_PARAMS,
        CHUNK,
        TRAILER,
        END,
        CLOSE,  // The associated stream/endpoint should be closed
        CLOSED  // The associated stream/endpoint is at EOF
    }


    /* Event Handler interface
     * These methods return true if the caller should process the events
     * so far received (eg return from parseNext and call HttpChannel.handle).
     * If multiple callbacks are called in sequence (eg
     * headerComplete then messageComplete) from the same point in the parsing
     * then it is sufficient for the caller to process the events only once.
     */
    public interface HttpHandler {
        boolean content(ByteBuffer item);

        boolean headerComplete();

        boolean contentComplete();

        boolean messageComplete();

        /**
         * This is the method called by parser when a HTTP Header name and value is found
         *
         * @param field The field parsed
         */
        void parsedHeader(HttpField field);

        /**
         * This is the method called by parser when a HTTP Trailer name and value is found
         *
         * @param field The field parsed
         */
        default void parsedTrailer(HttpField field) {
        }


        /**
         * Called to signal that an EOF was received unexpectedly
         * during the parsing of an HTTP message
         */
        void earlyEOF();


        /**
         * Called to signal that a bad HTTP message has been received.
         *
         * @param failure the failure with the bad message information
         */
        default void badMessage(BadMessageException failure) {
            badMessage(failure.getCode(), failure.getReason());
        }

        /**
         * @deprecated use {@link #badMessage(BadMessageException)} instead
         */
        @Deprecated
        default void badMessage(int status, String reason) {
        }


        /**
         * @return the size in bytes of the per parser header cache
         */
        int getHeaderCacheSize();
    }


    public interface RequestHandler extends HttpHandler {
        /**
         * This is the method called by parser when the HTTP request line is parsed
         *
         * @param method  The method
         * @param uri     The raw bytes of the URI.  These are copied into a ByteBuffer that will not be changed until this parser is reset and reused.
         * @param version the http version in use
         * @return true if handling parsing should return.
         */
        boolean startRequest(String method, String uri, HttpVersion version);

    }


    public interface ResponseHandler extends HttpHandler {
        /**
         * This is the method called by parser when the HTTP request line is parsed
         *
         * @param version the http version in use
         * @param status  the response status
         * @param reason  the response reason phrase
         * @return true if handling parsing should return
         */
        boolean startResponse(HttpVersion version, int status, String reason);
    }


    public interface ComplianceHandler extends HttpHandler {
        @Deprecated
        default void onComplianceViolation(HttpCompliance compliance, HttpCompliance required, String reason) {
        }

        default void onComplianceViolation(HttpCompliance compliance, HttpComplianceSection violation, String details) {
            onComplianceViolation(compliance, HttpCompliance.requiredCompliance(violation), details);
        }
    }


    private static class IllegalCharacterException extends BadMessageException {
        private IllegalCharacterException(State state, HttpTokens.Token token, ByteBuffer buffer) {
            super(400, String.format("Illegal character %s", token));
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Illegal character %s in state=%s for buffer %s", token, state, BufferUtils.toDetailString(buffer)));
            }
        }
    }
}
