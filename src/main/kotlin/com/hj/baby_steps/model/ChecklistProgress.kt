package com.hj.baby_steps.model

import jakarta.persistence.*
import java.util.*


@Entity
class ChecklistProgress (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val uuid: UUID,
    val checklistItemId: MutableSet<String> = mutableSetOf()
) {

    companion object {
        fun create() : ChecklistProgress{
            return ChecklistProgress(
                uuid = UUID.randomUUID(),
            )
        }
    }
}