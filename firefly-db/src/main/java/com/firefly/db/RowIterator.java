package com.firefly.db;

import com.firefly.utils.lang.AbstractLookaheadIterator;

/**
 * @author Pengtao Qiu
 */
public class RowIterator extends AbstractLookaheadIterator<Row> {

    private final SQLResultSet sqlResultSet;

    public RowIterator(SQLResultSet sqlResultSet) {
        this.sqlResultSet = sqlResultSet;
    }

    @Override
    protected Row loadNext() {
        return sqlResultSet.next() ? sqlResultSet : null;
    }
}
