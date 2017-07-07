package com.firefly.kotlin.ext.context

import com.firefly.annotation.Component
import com.firefly.annotation.Inject

/**
 * @author Pengtao Qiu
 */
@Component
class PigService {

    @Inject
    lateinit var fuckService: FuckService

    fun woo() = fuckService.fuck()
}