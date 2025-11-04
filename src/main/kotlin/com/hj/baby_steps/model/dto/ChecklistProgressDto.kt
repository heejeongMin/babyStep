package com.hj.baby_steps.model.dto

import java.util.*

data class ChecklistProgressDto(
    val id: UUID,
    val babyId: UUID,
    val checklistItemId: String,
    val completed: Boolean
) {
    companion object {
        fun to(
            id: UUID,
            babyUUID: UUID,
            checklistItemId: Set<String>
        ): List<ChecklistProgressDto> {
            return checklistItemId.map { ChecklistProgressDto(id, babyUUID, it, true) }
        }
    }
}