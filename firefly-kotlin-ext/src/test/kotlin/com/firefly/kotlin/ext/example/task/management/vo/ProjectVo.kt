package com.firefly.kotlin.ext.example.task.management.vo

import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
data class ProjectEditor(val project: Project,
                         val userIdList: List<Long>)


data class ProjectResult(val project: Project,
                         val users: List<User>)


