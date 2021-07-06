package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;

import java.util.*;

/**
 * Byte range inclusive of end points.
 * &lt;PRE&gt;
 * &lt;p&gt;
 * parses the following types of byte ranges:
 * &lt;p&gt;
 * bytes=100-499
 * bytes=-300
 * bytes=100-
 * bytes=1-2,2-3,6-,-2
 * &lt;p&gt;
 * given an entity length, converts range to string
 * &lt;p&gt;
 * bytes 100-499/500
 *
 * &lt;/PRE&gt;
 * &lt;p&gt;
 * Based on RFC2616 3.12, 14.16, 14.35.1, 14.35.2
 * &lt;p&gt;
 * And yes the spec does strangely say that while 10-20, is bytes 10 to 20 and 10- is bytes 10 until the end that -20 IS NOT bytes 0-20, but the last 20 bytes of the content.
 */
public class InclusiveByteRange {
    public static final LazyLogger LOG = SystemLogger.create(InclusiveByteRange.class);

    private long first;
    private long last;

    public InclusiveByteRange(long first, long last) {
        this.first = first;
        this.last = last;
    }

    public long getFirst() {
        return first;
    }

    public long getLast() {
        return last;
    }

    private void coalesce(InclusiveByteRange r) {
        first = Math.min(first, r.first);
        last = Math.max(last, r.last);
    }

    private boolean overlaps(InclusiveByteRange range) {
        return (range.first >= this.first && range.first <= this.last) ||
                (range.last >= this.first && range.last <= this.last) ||
                (range.first < this.first && range.last > this.last);
    }

    public long getSize() {
        return last - first + 1;
    }

    public String toHeaderRangeString(long size) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("bytes ");
        sb.append(first);
        sb.append('-');
        sb.append(last);
        sb.append("/");
        sb.append(size);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return (int) (first ^ last);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof InclusiveByteRange))
            return false;

        return ((InclusiveByteRange) obj).first == this.first &&
                ((InclusiveByteRange) obj).last == this.last;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(60);
        sb.append(first);
        sb.append(":");
        sb.append(last);
        return sb.toString();
    }

    public static List<InclusiveByteRange> satisfiableRanges(List<String> headers, long size) {
        Iterator<String> iterator = headers.iterator();
        Enumeration<String> enumeration = new Enumeration<String>() {

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
        return satisfiableRanges(enumeration, size);
    }

    /**
     * @param headers Enumeration of Range header fields.
     * @param size    Size of the resource.
     * @return List of satisfiable ranges
     */
    public static List<InclusiveByteRange> satisfiableRanges(Enumeration<String> headers, long size) {
        List<InclusiveByteRange> ranges = null;
        final long end = size - 1;

        // walk through all Range headers
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            StringTokenizer tok = new StringTokenizer(header, "=,", false);
            String t = null;
            try {
                // read all byte ranges for this header 
                while (tok.hasMoreTokens()) {
                    try {
                        t = tok.nextToken().trim();
                        if ("bytes".equals(t))
                            continue;

                        long first = -1;
                        long last = -1;
                        int dash = t.indexOf('-');
                        if (dash < 0 || t.indexOf("-", dash + 1) >= 0) {
                            LOG.warn("Bad range format: {}", t);
                            break;
                        }

                        if (dash > 0)
                            first = Long.parseLong(t.substring(0, dash).trim());
                        if (dash < (t.length() - 1))
                            last = Long.parseLong(t.substring(dash + 1).trim());

                        if (first == -1) {
                            if (last == -1) {
                                LOG.warn("Bad range format: {}", t);
                                break;
                            }

                            if (last == 0)
                                continue;

                            // This is a suffix range
                            first = Math.max(0, size - last);
                            last = end;
                        } else {
                            // Range starts after end
                            if (first >= size)
                                continue;

                            if (last == -1)
                                last = end;
                            else if (last >= end)
                                last = end;
                        }

                        if (last < first) {
                            LOG.warn("Bad range format: {}", t);
                            break;
                        }

                        InclusiveByteRange range = new InclusiveByteRange(first, last);
                        if (ranges == null)
                            ranges = new ArrayList<>();

                        boolean coalesced = false;
                        for (Iterator<InclusiveByteRange> i = ranges.listIterator(); i.hasNext(); ) {
                            InclusiveByteRange r = i.next();
                            if (range.overlaps(r)) {
                                coalesced = true;
                                r.coalesce(range);
                                while (i.hasNext()) {
                                    InclusiveByteRange r2 = i.next();

                                    if (r2.overlaps(r)) {
                                        r.coalesce(r2);
                                        i.remove();
                                    }
                                }
                            }
                        }

                        if (!coalesced)
                            ranges.add(range);
                    } catch (NumberFormatException e) {
                        LOG.warn("Bad range format: {}", t);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Bad range format: {}", t);
            }
        }

        return ranges;
    }

    public static String to416HeaderRangeString(long size) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("bytes */");
        sb.append(size);
        return sb.toString();
    }
}



