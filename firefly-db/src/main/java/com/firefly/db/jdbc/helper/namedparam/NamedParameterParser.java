package com.firefly.db.jdbc.helper.namedparam;

import com.firefly.utils.Assert;
import com.firefly.utils.BeanUtils;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.lang.bean.PropertyAccess;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class NamedParameterParser {
    /**
     * Set of characters that qualify as parameter separators,
     * indicating that a parameter name in a SQL String has ended.
     */
    private static final char[] PARAMETER_SEPARATORS =
            new char[]{'"', '\'', ':', '&', ',', ';', '(', ')', '|', '=', '+', '-', '*', '%', '/', '\\', '<', '>', '^'};

    /**
     * Set of characters that qualify as comment or quotes starting characters.
     */
    private static final String[] START_SKIP =
            new String[]{"'", "\"", "--", "/*"};

    /**
     * Set of characters that at are the corresponding comment or quotes ending characters.
     */
    private static final String[] STOP_SKIP =
            new String[]{"'", "\"", "\n", "*/"};

    /**
     * Parse the SQL statement and locate any placeholders or named parameters.
     * Named parameters are substituted for a JDBC placeholder.
     *
     * @param sql the SQL statement
     * @return the parsed statement, represented as ParsedSql instance
     */
    public static ParsedSql parseSqlStatement(final String sql) {
        Assert.notNull(sql, "SQL must not be null");

        Set<String> namedParameters = new HashSet<>();
        String sqlToUse = sql;
        List<ParameterHolder> parameterList = new ArrayList<>();

        char[] statement = sql.toCharArray();
        int namedParameterCount = 0;
        int unnamedParameterCount = 0;
        int totalParameterCount = 0;

        int escapes = 0;
        int i = 0;
        while (i < statement.length) {
            int skipToPosition;
            while (i < statement.length) {
                skipToPosition = skipCommentsAndQuotes(statement, i);
                if (i == skipToPosition) {
                    break;
                } else {
                    i = skipToPosition;
                }
            }
            if (i >= statement.length) {
                break;
            }
            char c = statement[i];
            if (c == ':' || c == '&') {
                int j = i + 1;
                if (j < statement.length && statement[j] == ':' && c == ':') {
                    // Postgres-style "::" casting operator should be skipped
                    i = i + 2;
                    continue;
                }
                String parameter;
                if (j < statement.length && c == ':' && statement[j] == '{') {
                    // :{x} style parameter
                    while (j < statement.length && !('}' == statement[j])) {
                        j++;
                        if (':' == statement[j] || '{' == statement[j]) {
                            throw new CommonRuntimeException("Parameter name contains invalid character '" +
                                    statement[j] + "' at position " + i + " in statement: " + sql);
                        }
                    }
                    if (j >= statement.length) {
                        throw new CommonRuntimeException(
                                "Non-terminated named parameter declaration at position " + i + " in statement: " + sql);
                    }
                    if (j - i > 3) {
                        parameter = sql.substring(i + 2, j);
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
                        totalParameterCount = addNamedParameter(parameterList, totalParameterCount, escapes, i, j + 1, parameter);
                    }
                    j++;
                } else {
                    while (j < statement.length && !isParameterSeparator(statement[j])) {
                        j++;
                    }
                    if (j - i > 1) {
                        parameter = sql.substring(i + 1, j);
                        namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
                        totalParameterCount = addNamedParameter(parameterList, totalParameterCount, escapes, i, j, parameter);
                    }
                }
                i = j - 1;
            } else {
                if (c == '\\') {
                    int j = i + 1;
                    if (j < statement.length && statement[j] == ':') {
                        // escaped ":" should be skipped
                        sqlToUse = sqlToUse.substring(0, i - escapes) + sqlToUse.substring(i - escapes + 1);
                        escapes++;
                        i = i + 2;
                        continue;
                    }
                }
                if (c == '?') {
                    int j = i + 1;
                    if (j < statement.length && (statement[j] == '?' || statement[j] == '|' || statement[j] == '&')) {
                        // Postgres-style "??", "?|", "?&" operator should be skipped
                        i = i + 2;
                        continue;
                    }
                    unnamedParameterCount++;
                    totalParameterCount++;
                }
            }
            i++;
        }

        ParsedSql parsedSql = new ParsedSql(sqlToUse);
        parsedSql.setParameterList(parameterList);
        parsedSql.setNamedParameterCount(namedParameterCount);
        parsedSql.setUnnamedParameterCount(unnamedParameterCount);
        parsedSql.setTotalParameterCount(totalParameterCount);
        return parsedSql;
    }

    private static int addNamedParameter(List<ParameterHolder> parameterList,
                                         int totalParameterCount, int escapes,
                                         int i, int j, String parameter) {
        parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
        totalParameterCount++;
        return totalParameterCount;
    }

    private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
        if (!namedParameters.contains(parameter)) {
            namedParameters.add(parameter);
            namedParameterCount++;
        }
        return namedParameterCount;
    }

    /**
     * Skip over comments and quoted names present in an SQL statement
     *
     * @param statement character array containing SQL statement
     * @param position  current position of statement
     * @return next position to process after any comments or quotes are skipped
     */
    private static int skipCommentsAndQuotes(char[] statement, int position) {
        for (int i = 0; i < START_SKIP.length; i++) {
            if (statement[position] == START_SKIP[i].charAt(0)) {
                boolean match = true;
                for (int j = 1; j < START_SKIP[i].length(); j++) {
                    if (!(statement[position + j] == START_SKIP[i].charAt(j))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    int offset = START_SKIP[i].length();
                    for (int m = position + offset; m < statement.length; m++) {
                        if (statement[m] == STOP_SKIP[i].charAt(0)) {
                            boolean endMatch = true;
                            int endPos = m;
                            for (int n = 1; n < STOP_SKIP[i].length(); n++) {
                                if (m + n >= statement.length) {
                                    // last comment not closed properly
                                    return statement.length;
                                }
                                if (!(statement[m + n] == STOP_SKIP[i].charAt(n))) {
                                    endMatch = false;
                                    break;
                                }
                                endPos = m + n;
                            }
                            if (endMatch) {
                                // found character sequence ending comment or quote
                                return endPos + 1;
                            }
                        }
                    }
                    // character sequence ending comment or quote not found
                    return statement.length;
                }

            }
        }
        return position;
    }

    /**
     * Determine whether a parameter name ends at the current position,
     * that is, whether the given character qualifies as a separator.
     */
    private static boolean isParameterSeparator(char c) {
        if (Character.isWhitespace(c)) {
            return true;
        }
        for (char separator : PARAMETER_SEPARATORS) {
            if (c == separator) {
                return true;
            }
        }
        return false;
    }

    public static PreparedSqlAndValues replaceParsedSql(ParsedSql parsedSql, Map<String, Object> paramMap) {
        final String originalSql = parsedSql.getOriginalSql();
        if (CollectionUtils.isEmpty(parsedSql.getParameterList())) {
            return new PreparedSqlAndValues(originalSql, Collections.emptyList());
        }

        StringBuilder actualSql = new StringBuilder(originalSql.length());
        List<Object> values = new ArrayList<>();
        int lastIndex = 0;
        for (ParameterHolder param : parsedSql.getParameterList()) {
            actualSql.append(originalSql, lastIndex, param.getStartIndex());
            Object value = Optional.ofNullable(paramMap).map(map -> map.get(param.getParameterName())).orElse(null);
            if (value == null) {
                actualSql.append('?');
            } else {
                if (value instanceof Collection) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> objects = (Collection<Object>) value;
                    actualSql.append(objects.parallelStream().map(o -> "?").collect(Collectors.joining(",")));
                    values.addAll(objects);
                } else {
                    actualSql.append('?');
                    values.add(value);
                }
            }
            lastIndex = param.getEndIndex();
        }
        actualSql.append(originalSql, lastIndex, originalSql.length());
        return new PreparedSqlAndValues(actualSql.toString(), values);
    }

    /**
     * Get parsed SQL and values.
     *
     * @param sql      The named parameter SQL.
     * @param paramMap The parameters.
     * @return parsed SQL and values.
     */
    public static PreparedSqlAndValues getPreparedSqlAndValues(String sql, Map<String, Object> paramMap) {
        return replaceParsedSql(parseSqlStatement(sql), paramMap);
    }

    /**
     * Get parsed SQL and values.
     *
     * @param sql    The named parameter SQL.
     * @param object The parameters.
     * @return parsed SQL and values.
     */
    public static PreparedSqlAndValues getPreparedSqlAndValues(String sql, Object object) {
        Map<String, PropertyAccess> beanAccess = BeanUtils.getBeanAccess(object.getClass());
        Map<String, Object> paramMap = new HashMap<>();
        beanAccess.forEach((name, property) -> paramMap.put(name, property.getValue(object)));
        return getPreparedSqlAndValues(sql, paramMap);
    }
}
