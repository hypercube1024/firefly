package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.kotlin.ext.common.Json
import java.util.*

/**
 * @author Pengtao Qiu
 */
class Page<T> {

    private var record: MutableList<T> = ArrayList()
    private var number: Int = 0
    private var size: Int = 0
    var total: Int = 0

    var pageCount: Int = 0
    var lastNumber: Int = 0
    var nextNumber: Int = 0
    var isShowPaging: Boolean = false
    var isNext: Boolean = false
    var isLast: Boolean = false

    constructor() {}

    constructor(record: MutableList<T>, number: Int, size: Int) {
        this.record = record
        this.size = Math.max(size, 0)
        this.number = Math.max(number, 1)
        pageWithoutCount()
    }

    constructor(record: MutableList<T>, total: Int, number: Int, size: Int) {
        this.record = record
        this.total = total
        this.size = Math.max(size, 0)
        this.number = Math.max(number, 1)
        page()
    }

    fun pageWithoutCount() {
        isLast = number > 1
        isNext = record.size > size
        isShowPaging = isLast || isNext
        lastNumber = Math.max(number - 1, 1)
        nextNumber = if (isNext) number + 1 else number

        if (isNext) {
            record.removeAt(record.size - 1)
        }
    }

    fun page() {
        pageCount = (total + size - 1) / size
        isLast = number > 1
        isNext = number < pageCount
        isShowPaging = isLast || isNext
        lastNumber = Math.max(number - 1, 1)
        nextNumber = Math.min(number + 1, pageCount)
    }

    fun getRecord(): List<T> {
        return record
    }

    fun setRecord(record: MutableList<T>) {
        this.record = record
    }

    fun getNumber(): Int {
        return number
    }

    fun setNumber(number: Int) {
        this.number = Math.max(number, 1)
    }

    fun getSize(): Int {
        return size
    }

    fun setSize(size: Int) {
        this.size = Math.max(size, 0)
    }

    override fun toString(): String {
        return Json.toJson(this)
    }

    companion object {
        fun getPageSQLWithoutCount(number: Int, size: Int): String {
            val offset = (Math.max(number - 1, 0) * size).toLong()
            val pageSize = size + 1
            return " limit $offset, $pageSize"
        }

        fun getPageSQL(number: Int, size: Int): String {
            val offset = (Math.max(number - 1, 0) * size).toLong()
            return " limit $offset, $size"
        }
    }
}