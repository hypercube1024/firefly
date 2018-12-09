package com.fireflysource.net.http.common.model;

import com.fireflysource.common.string.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements a quoted comma separated list of values
 * in accordance with RFC7230.
 * OWS is removed and quoted characters ignored for parsing.
 *
 * @see "https://tools.ietf.org/html/rfc7230#section-3.2.6"
 * @see "https://tools.ietf.org/html/rfc7230#section-7"
 */
public class QuotedCSV implements Iterable<String> {

    protected final List<String> values = new ArrayList<>();
    protected final boolean keepQuotes;

    public QuotedCSV(String... values) {
        this(true, values);
    }

    public QuotedCSV(boolean keepQuotes, String... values) {
        this.keepQuotes = keepQuotes;
        for (String v : values)
            addValue(v);
    }

    public static String unquote(String s) {
        if (!StringUtils.hasText(s)) {
            return s;
        }
        // handle trivial cases
        int l = s.length();
        // Look for any quotes
        int i = 0;
        for (; i < l; i++) {
            char c = s.charAt(i);
            if (c == '"')
                break;
        }
        if (i == l)
            return s;

        boolean quoted = true;
        boolean sloshed = false;
        StringBuilder buffer = new StringBuilder();
        buffer.append(s, 0, i);
        i++;
        for (; i < l; i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (sloshed) {
                    buffer.append(c);
                    sloshed = false;
                } else if (c == '"')
                    quoted = false;
                else if (c == '\\')
                    sloshed = true;
                else
                    buffer.append(c);
            } else if (c == '"')
                quoted = true;
            else
                buffer.append(c);
        }
        return buffer.toString();
    }

    /**
     * Add and parse a value string(s)
     *
     * @param value A value that may contain one or more Quoted CSV items.
     */
    public void addValue(String value) {
        if (value == null)
            return;

        StringBuilder buffer = new StringBuilder();

        int l = value.length();
        State state = State.VALUE;
        boolean quoted = false;
        boolean sloshed = false;
        int nws_length = 0;
        int last_length = 0;
        int value_length = -1;
        int param_name = -1;
        int param_value = -1;

        for (int i = 0; i <= l; i++) {
            char c = i == l ? 0 : value.charAt(i);

            // Handle quoting https://tools.ietf.org/html/rfc7230#section-3.2.6
            if (quoted && c != 0) {
                if (sloshed)
                    sloshed = false;
                else {
                    switch (c) {
                        case '\\':
                            sloshed = true;
                            if (!keepQuotes)
                                continue;
                            break;
                        case '"':
                            quoted = false;
                            if (!keepQuotes)
                                continue;
                            break;
                    }
                }

                buffer.append(c);
                nws_length = buffer.length();
                continue;
            }

            // Handle common cases
            switch (c) {
                case ' ':
                case '\t':
                    if (buffer.length() > last_length) // not leading OWS
                        buffer.append(c);
                    continue;

                case '"':
                    quoted = true;
                    if (keepQuotes) {
                        if (state == State.PARAM_VALUE && param_value < 0)
                            param_value = nws_length;
                        buffer.append(c);
                    } else if (state == State.PARAM_VALUE && param_value < 0)
                        param_value = nws_length;
                    nws_length = buffer.length();
                    continue;

                case ';':
                    buffer.setLength(nws_length); // trim following OWS
                    if (state == State.VALUE) {
                        parsedValue(buffer);
                        value_length = buffer.length();
                    } else
                        parsedParam(buffer, value_length, param_name, param_value);
                    nws_length = buffer.length();
                    param_name = param_value = -1;
                    buffer.append(c);
                    last_length = ++nws_length;
                    state = State.PARAM_NAME;
                    continue;

                case ',':
                case 0:
                    if (nws_length > 0) {
                        buffer.setLength(nws_length); // trim following OWS
                        switch (state) {
                            case VALUE:
                                parsedValue(buffer);
//                                value_length = buffer.length();
                                break;
                            case PARAM_NAME:
                            case PARAM_VALUE:
                                parsedParam(buffer, value_length, param_name, param_value);
                                break;
                        }
                        values.add(buffer.toString());
                    }
                    buffer.setLength(0);
                    last_length = 0;
                    nws_length = 0;
                    value_length = param_name = param_value = -1;
                    state = State.VALUE;
                    continue;

                case '=':
                    switch (state) {
                        case VALUE:
                            // It wasn't really a value, it was a param name
                            value_length = param_name = 0;
                            buffer.setLength(nws_length); // trim following OWS
                            String param = buffer.toString();
                            buffer.setLength(0);
                            parsedValue(buffer);
                            value_length = buffer.length();
                            buffer.append(param);
                            buffer.append(c);
                            last_length = ++nws_length;
                            state = State.PARAM_VALUE;
                            continue;

                        case PARAM_NAME:
                            buffer.setLength(nws_length); // trim following OWS
                            buffer.append(c);
                            last_length = ++nws_length;
                            state = State.PARAM_VALUE;
                            continue;

                        case PARAM_VALUE:
                            if (param_value < 0)
                                param_value = nws_length;
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue;
                    }
                    continue;

                default: {
                    switch (state) {
                        case VALUE: {
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue;
                        }

                        case PARAM_NAME: {
                            if (param_name < 0)
                                param_name = nws_length;
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue;
                        }

                        case PARAM_VALUE: {
                            if (param_value < 0)
                                param_value = nws_length;
                            buffer.append(c);
                            nws_length = buffer.length();
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a value has been parsed
     *
     * @param buffer Containing the trimmed value, which may be mutated
     */
    protected void parsedValue(StringBuilder buffer) {
    }

    /**
     * Called when a parameter has been parsed
     *
     * @param buffer      Containing the trimmed value and all parameters, which may be mutated
     * @param valueLength The length of the value
     * @param paramName   The index of the start of the parameter just parsed
     * @param paramValue  The index of the start of the parameter value just parsed, or -1
     */
    protected void parsedParam(StringBuilder buffer, int valueLength, int paramName, int paramValue) {
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public Iterator<String> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        for (String s : this) {
            list.add(s);
        }
        return list.toString();
    }

    private enum State {VALUE, PARAM_NAME, PARAM_VALUE}
}
