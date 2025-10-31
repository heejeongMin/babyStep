package com.hj.baby_steps.model.dto

import java.util.*

data class CheckListProgressDto (
    val id: UUID,
    val babyId: UUID,
    val checkListItemId: Long,
    val completed: Boolean
)