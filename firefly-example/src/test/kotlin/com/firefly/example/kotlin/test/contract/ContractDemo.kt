package com.firefly.example.kotlin.test.contract

/**
 * @author Pengtao Qiu
 */

fun main(args: Array<String>) {
    printStr("hello")
    printStr(null)
}

fun printStr(str: String?) {
    require(!str.isNullOrBlank()) { "The argument must be not blank" }
    println("Str len: ${str.length}")
}