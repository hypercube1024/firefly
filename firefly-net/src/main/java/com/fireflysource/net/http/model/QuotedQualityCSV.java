package com.fireflysource.net.http.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static java.lang.Integer.MIN_VALUE;

/**
 * Implements a quoted comma separated list of quality values in accordance with
 * RFC7230 and RFC7231. Values are returned sorted in quality order, with OWS
 * and the quality parameters removed.
 *
 * @see "https://tools.ietf.org/html/rfc7230#section-3.2.6"
 * @see "https://tools.ietf.org/html/rfc7230#section-7"
 * @see "https://tools.ietf.org/html/rfc7231#section-5.3.1"
 */
public class QuotedQualityCSV extends QuotedCSV implements Iterable<String> {
    private final static Double ZERO = 0.0;
    private final static Double ONE = 1.0;

    /**
     * Function to apply a most specific MIME encoding secondary ordering
     */
    public static Function<String, Integer> MOST_SPECIFIC = s -> {
        String[] elements = s.split("/");
        return 1000000 * elements.length + 1000 * elements[0].length() + elements[elements.length - 1].length();
    };

    private final List<Double> quality = new ArrayList<>();
    private boolean sorted = false;
    private final Function<String, Integer> secondaryOrdering;


    /**
     * Sorts values with equal quality according to the length of the value String.
     */
    public QuotedQualityCSV() {
        this((s) -> 0);
    }

    /**
     * Sorts values with equal quality according to given order.
     *
     * @param preferredOrder Array indicating the preferred order of known values
     */
    public QuotedQualityCSV(String[] preferredOrder) {
        this((s) -> {
            for (int i = 0; i < preferredOrder.length; ++i)
                if (preferredOrder[i].equals(s))
                    return preferredOrder.length - i;

            if ("*".equals(s))
                return preferredOrder.length;

            return MIN_VALUE;
        });
    }

    /**
     * Orders values with equal quality with the given function.
     *
     * @param secondaryOrdering Function to apply an ordering other than specified by quality
     */
    public QuotedQualityCSV(Function<String, Integer> secondaryOrdering) {
        this.secondaryOrdering = secondaryOrdering;
    }

    @Override
    protected void parsedValue(StringBuffer buffer) {
        super.parsedValue(buffer);
        quality.add(ONE);
    }

    @Override
    protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
        if (paramName < 0) {
            if (buffer.charAt(buffer.length() - 1) == ';') {
                buffer.setLength(buffer.length() - 1);
            }
        } else if (paramValue >= 0 &&
                buffer.charAt(paramName) == 'q' && paramValue > paramName &&
                buffer.length() >= paramName && buffer.charAt(paramName + 1) == '=') {
            Double q;
            try {
                q = (keepQuotes && buffer.charAt(paramValue) == '"')
                        ? new Double(buffer.substring(paramValue + 1, buffer.length() - 1))
                        : new Double(buffer.substring(paramValue));
            } catch (Exception e) {
                q = ZERO;
            }
            buffer.setLength(Math.max(0, paramName - 1));

            if (!ONE.equals(q)) {
                quality.set(quality.size() - 1, q);
            }
        }
    }

    public List<String> getValues() {
        if (!sorted) {
            sort();
        }
        return values;
    }

    @Override
    public Iterator<String> iterator() {
        if (!sorted) {
            sort();
        }
        return values.iterator();
    }

    protected void sort() {
        sorted = true;

        Double last = ZERO;
        int lastSecondaryOrder = Integer.MIN_VALUE;

        for (int i = values.size(); i-- > 0; ) {
            String v = values.get(i);
            Double q = quality.get(i);

            int compare = last.compareTo(q);
            if (compare > 0 || (compare == 0 && secondaryOrdering.apply(v) < lastSecondaryOrder)) {
                values.set(i, values.get(i + 1));
                values.set(i + 1, v);
                quality.set(i, quality.get(i + 1));
                quality.set(i + 1, q);
                last = ZERO;
                lastSecondaryOrder = 0;
                i = values.size();
                continue;
            }

            last = q;
            lastSecondaryOrder = secondaryOrdering.apply(v);
        }

        int last_element = quality.size();
        while (last_element > 0 && quality.get(--last_element).equals(ZERO)) {
            quality.remove(last_element);
            values.remove(last_element);
        }
    }
}
