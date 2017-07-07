package com.firefly.kotlin.ext.context

import com.firefly.core.ApplicationContext
import com.firefly.kotlin.ext.common.firefly

/**
 * Application context extension
 *
 * @author Pengtao Qiu
 */

inline fun <reified T> ApplicationContext.getBean(): T = this.getBean(T::class.java)

inline fun <reified T> ApplicationContext.getBeans(): Collection<T> = this.getBeans(T::class.java)

object Context {

    inline fun <reified T> getBean(): T = firefly.getBean(T::class.java)

    fun <T> getBean(id: String): T = firefly.getBean(id)

    inline fun <reified T> getBeans(): Collection<T> = firefly.getBeans(T::class.java)

    fun getBeanMap(): Map<String, Any> = firefly.getBeanMap()

    fun create(): ApplicationContext = firefly.createApplicationContext()

    fun create(path: String): ApplicationContext = firefly.createApplicationContext(path)
}