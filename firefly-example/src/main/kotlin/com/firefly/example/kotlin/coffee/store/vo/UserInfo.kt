package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.kotlin.ext.annotation.NoArg
import java.io.Serializable

/**
 * @author Pengtao Qiu
 */
@NoArg
data class UserInfo(
    var id: Long,
    var name: String
                   ) : Serializable