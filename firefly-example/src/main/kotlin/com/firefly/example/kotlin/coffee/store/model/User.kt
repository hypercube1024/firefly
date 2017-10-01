package com.firefly.example.kotlin.coffee.store.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "user", catalog = "coffee_store")
data class User(@Id("id") var id: Long,
                @Column("name") var name: String,
                @Column("password") var password: String,
                @Column("create_time") var createTime: Date,
                @Column("update_time") var updateTime: Date)