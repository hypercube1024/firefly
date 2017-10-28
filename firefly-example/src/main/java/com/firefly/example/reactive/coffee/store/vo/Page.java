package com.firefly.example.reactive.coffee.store.vo;

import com.firefly.$;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class Page<T> {

    private List<T> record = new ArrayList<>();
    private int number, size, total;

    private int pageCount, lastNumber, nextNumber;
    private boolean showPaging, next, last;

    public Page() {
    }

    public Page(List<T> record, int number, int size) {
        this.record = record;
        this.size = Math.max(size, 0);
        this.number = Math.max(number, 1);
        pageWithoutCount();
    }

    public Page(List<T> record, int total, int number, int size) {
        this.record = record;
        this.total = total;
        this.size = Math.max(size, 0);
        this.number = Math.max(number, 1);
        page();
    }

    public static String getPageSQLWithoutCount(int number, int size) {
        long offset = Math.max(number - 1, 0) * size;
        int pageSize = size + 1;
        return " limit " + offset + ", " + pageSize;
    }

    public static String getPageSQL(int number, int size) {
        long offset = Math.max(number - 1, 0) * size;
        return " limit " + offset + ", " + size;
    }

    public void pageWithoutCount() {
        last = number > 1;
        next = record.size() > size;
        showPaging = last || next;
        lastNumber = Math.max(number - 1, 1);
        nextNumber = next ? number + 1 : number;

        if (next) {
            record.remove(record.size() - 1);
        }
    }

    public void page() {
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
        this.number = Math.max(number, 1);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.max(size, 0);
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

    @Override
    public String toString() {
        return $.json.toJson(this);
    }
}
