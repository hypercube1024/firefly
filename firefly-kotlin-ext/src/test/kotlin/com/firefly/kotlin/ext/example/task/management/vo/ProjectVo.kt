package com.firefly.kotlin.ext.example.task.management.vo

import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
@NoArg
data class ProjectEditor(
    var project: Project,
    var userIdList: List<Long>
                        )

@NoArg
data class ProjectResult(
    var project: Project,
    var users: List<User>
                        )
