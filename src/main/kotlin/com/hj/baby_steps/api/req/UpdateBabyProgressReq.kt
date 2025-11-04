package com.hj.baby_steps.api.req

data class UpdateBabyProgressReq (
    val checklistItemId: String,
    val completed: Boolean
)