package com.firefly.example.reactive.coffee.store.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class Page<T> {

    private List<T> record = new ArrayList<>();
    private int total, pageCount, lastNumber, nextNumber, number, size;
    private boolean showPaging, next, last;

    public Page() {
    }

    public Page(List<T> record, int size, int number) {
        // paging without total row number
        this.record = record;
        this.size = size;
        this.number = number;

        last = number > 1;
        next = record.size() > size;
        showPaging = last || next;
        lastNumber = Math.max(number - 1, 1);
        nextNumber = next ? number + 1 : number;

        if (next) {
            record.remove(record.size() - 1);
        }
    }

    public Page(List<T> record, int total, int size, int number) {
        this.record = record;
        this.total = total;
        this.size = size;
        this.number = number;

        pageCount = (total + size - 1) / size;
        last = number > 1;
        next = number < pageCount;
        showPaging = last || next;
        lastNumber = Math.max(number - 1, 1);
        nextNumber = Math.min(number + 1, pageCount);
    }

    public List<T> getRecord() {
        return record;
    }

    public void setRecord(List<T> record) {
        this.record = record;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(int lastNumber) {
        this.lastNumber = lastNumber;
    }

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isShowPaging() {
        return showPaging;
    }

    public void setShowPaging(boolean showPaging) {
        this.showPaging = showPaging;
    }

    public boolean isNext() {
        return next;
    }

    public void setNext(boolean next) {
        this.next = next;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
