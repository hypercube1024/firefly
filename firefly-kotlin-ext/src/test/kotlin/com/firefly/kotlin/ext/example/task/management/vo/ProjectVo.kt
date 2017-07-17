package com.firefly.kotlin.ext.example.task.management.vo

import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
@NoArg
data class ProjectEditor(val project: Project,
                         val userIdList: List<Long>)

@NoArg
data class ProjectResult(val project: Project,
                         val users: List<User>)


