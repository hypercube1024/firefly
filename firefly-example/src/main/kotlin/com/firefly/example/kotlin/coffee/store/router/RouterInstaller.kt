package com.firefly.example.kotlin.coffee.store.router

/**
 * @author Pengtao Qiu
 */
interface RouterInstaller : Comparable<RouterInstaller> {

    fun install()

    fun order() = Integer.MAX_VALUE

    override fun compareTo(other: RouterInstaller): Int = order().compareTo(other.order())
}