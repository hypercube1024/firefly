package com.firefly.example.reactive.coffee.store.vo;

/**
 * @author Pengtao Qiu
 */
public class ProductQuery {

    private Integer productStatus;
    private Integer productType;
    private String searchKey;
    private int pageNumber = 1;
    private int pageSize = 20;

    public ProductQuery() {
    }

    public ProductQuery(String searchKey, Integer productStatus, Integer productType, int pageNumber, int pageSize) {
        this.searchKey = searchKey;
        this.productStatus = productStatus;
        this.productType = productType;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public Integer getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(Integer productStatus) {
        this.productStatus = productStatus;
    }

    public Integer getProductType() {
        return productType;
    }

    public void setProductType(Integer productType) {
        this.productType = productType;
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
