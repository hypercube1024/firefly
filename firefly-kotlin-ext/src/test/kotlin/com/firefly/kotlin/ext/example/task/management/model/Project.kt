package com.firefly.kotlin.ext.example.task.management.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "project", catalog = "test")
data class Project(
    @Id("id") var id: Long?,
    @Column("name") var name: String,
    @Column("description") var description: String,
    @Column("create_time") var createTime: Date? = Date(),
    @Column("update_time") var updateTime: Date? = Date()
                  ) {

    override fun equals(other: Any?): Boolean {
        return if (other is Project) Objects.equals(id, other.id) else false
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}