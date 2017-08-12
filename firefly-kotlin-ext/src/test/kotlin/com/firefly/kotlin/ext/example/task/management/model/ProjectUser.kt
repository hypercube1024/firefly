package com.firefly.kotlin.ext.example.task.management.model

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table

/**
 * @author Pengtao Qiu
 */
@Table(value = "project_user", catalog = "test")
data class ProjectUser(@Id("id") var id: Long?,
                       @Column("user_id") var userId: Long,
                       @Column("project_id") var projectId: Long)