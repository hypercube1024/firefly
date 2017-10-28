package com.firefly.example.reactive.coffee.store.vo;

/**
 * @author Pengtao Qiu
 */
public class ProductQuery {

    private Integer status;
    private Integer type;
    private String searchKey;
    private int pageNumber = 1;
    private int pageSize = 20;

    public ProductQuery() {
    }

    public ProductQuery(String searchKey, Integer status, Integer type, int pageNumber, int pageSize) {
        this.searchKey = searchKey;
        this.status = status;
        this.type = type;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
