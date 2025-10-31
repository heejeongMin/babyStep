package com.hj.baby_steps.api.res

import com.hj.baby_steps.model.dto.BabyDto

data class GetBabyRes (
    val babies: List<BabyDto>
)