package com.firefly.db;


import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Pengtao Qiu
 */
public interface SQLResultSet extends Row, Iterable<Row> {

    boolean next();

    void close();

    @Override
    default Iterator<Row> iterator() {
        return new RowIterator(this);
    }

    default Stream<Row> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }
}
