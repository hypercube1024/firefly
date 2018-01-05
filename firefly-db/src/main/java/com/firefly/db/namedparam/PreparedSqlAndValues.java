package com.firefly.db.namedparam;

import java.util.Collections;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class PreparedSqlAndValues {

    private final String preparedSql;
    private final List<Object> values;

    public PreparedSqlAndValues(String preparedSql, List<Object> values) {
        this.preparedSql = preparedSql;
        this.values = values;
    }

    public String getPreparedSql() {
        return preparedSql;
    }

    public List<Object> getValues() {
        return Collections.unmodifiableList(values);
    }
}
