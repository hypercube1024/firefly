package com.firefly.example.kotlin.coffee.store

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class ProjectConfig(
        var templateRoot: String,
        var host: String,
        var port: Int,
        var loginURL: String,
        var logoutURL: String,
        var loginUserKey: String,
        var sessionMaxInactiveInterval: Int)
