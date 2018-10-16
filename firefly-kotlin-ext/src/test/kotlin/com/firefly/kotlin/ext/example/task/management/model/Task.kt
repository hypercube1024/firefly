package com.firefly.kotlin.ext.example.task.management.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Table(value = "task", catalog = "test")
data class Task(
    @Id("id") var id: Long,
    @Column("name") var name: String,
    @Column("start_time") var startTime: Date,
    @Column("end_time") var endTime: Date,
    @Column("create_time") var createTime: Date,
    @Column("update_time") var updateTime: Date,
    @Column("description") var description: String,
    @Column("status") var status: Int,
    @Column("user_id") var userId: Long,
    @Column("project_id") var projectId: Long
               ) {

    override fun equals(other: Any?): Boolean {
        return if (other is Task) Objects.equals(id, other.id) else false
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}

enum class TaskStatus(
    val value: Int,
    val description: String
                     ) {
    INIT(0, "init"),
    PROCESSING(1, "processing"),
    DONE(2, "done")
}