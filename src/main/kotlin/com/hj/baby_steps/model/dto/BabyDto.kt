package com.hj.baby_steps.model.dto

import com.hj.baby_steps.model.Baby
import java.time.LocalDate
import java.util.UUID

class BabyDto(
    val id: UUID,
    val name: String,
    val birthdate: LocalDate
) {
    companion object {
        fun from(baby: Baby): BabyDto{
            return BabyDto(
                baby.uuid,
                baby.name,
                baby.birthdate
            )
        }
    }
}