package com.firefly.db.namedparam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ParsedSql {

    private String originalSql;
    private List<ParameterHolder> parameterList = new ArrayList<>();
    private int namedParameterCount;
    private int unnamedParameterCount;
    private int totalParameterCount;


    /**
     * Create a new instance of the {@link ParsedSql} class.
     *
     * @param originalSql the SQL statement that is being (or is to be) parsed
     */
    ParsedSql(String originalSql) {
        this.originalSql = originalSql;
    }

    /**
     * Return the SQL statement that is being parsed.
     *
     * @return the SQL statement that is being parsed.
     */
    public String getOriginalSql() {
        return originalSql;
    }

    /**
     * Set the parameter list.
     *
     * @param parameterList The parameter list.
     */
    void setParameterList(List<ParameterHolder> parameterList) {
        this.parameterList = parameterList;
    }

    /**
     * Get the parameter list.
     *
     * @return the parameter list.
     */
    public List<ParameterHolder> getParameterList() {
        return Collections.unmodifiableList(parameterList);
    }

    /**
     * Return the parameter indexes for the specified parameter.
     *
     * @param parameterPosition the position of the parameter
     *                          (as index in the parameter names List)
     * @return the parameter
     */
    public ParameterHolder getParameter(int parameterPosition) {
        return parameterList.get(parameterPosition);
    }

    /**
     * Set the count of named parameters in the SQL statement.
     * Each parameter name counts once; repeated occurrences do not count here.
     */
    void setNamedParameterCount(int namedParameterCount) {
        this.namedParameterCount = namedParameterCount;
    }

    /**
     * Return the count of named parameters in the SQL statement.
     * Each parameter name counts once; repeated occurrences do not count here.
     *
     * @return the count of named parameters in the SQL statement.
     */
    public int getNamedParameterCount() {
        return this.namedParameterCount;
    }

    /**
     * Set the count of all of the unnamed parameters in the SQL statement.
     */
    void setUnnamedParameterCount(int unnamedParameterCount) {
        this.unnamedParameterCount = unnamedParameterCount;
    }

    /**
     * Return the count of all of the unnamed parameters in the SQL statement.
     *
     * @return the count of all of the unnamed parameters in the SQL statement.
     */
    public int getUnnamedParameterCount() {
        return this.unnamedParameterCount;
    }

    /**
     * Set the total count of all of the parameters in the SQL statement.
     * Repeated occurrences of the same parameter name do count here.
     */
    void setTotalParameterCount(int totalParameterCount) {
        this.totalParameterCount = totalParameterCount;
    }

    /**
     * Return the total count of all of the parameters in the SQL statement.
     * Repeated occurrences of the same parameter name do count here.
     *
     * @return the total count of all of the parameters in the SQL statement.
     */
    public int getTotalParameterCount() {
        return this.totalParameterCount;
    }

    /**
     * Exposes the original SQL String.
     */
    @Override
    public String toString() {
        return this.originalSql;
    }

}
