package com.firefly.kotlin.ext.example.task.management.vo

/**
 * @author Pengtao Qiu
 */
data class Request<out T>(val authToken: String,
                          val data: T)

data class Response<out T>(val code: Int,
                           val message: String,
                           val data: T?)