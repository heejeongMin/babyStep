package com.hj.baby_steps.service

//import com.hj.baby_steps.repository.BabiesRepository
import com.hj.baby_steps.model.Baby
import com.hj.baby_steps.model.dto.BabyDto
import com.hj.baby_steps.model.dto.CheckListProgressDto
import com.hj.baby_steps.repository.BabyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class BabyService(
    val babyRepository: BabyRepository
) {

    fun addBaby(name: String, birthdate: LocalDate): BabyDto =
        BabyDto.from(babyRepository.save(Baby.create(name, birthdate)))


    fun getBabies(): List<BabyDto> =
        babyRepository.findByIsActiveIsTrue().map { BabyDto.from(it) }


    fun getBaby(uuid: UUID): BabyDto =
        BabyDto.from(babyRepository.findByUuid(uuid) ?: throw Exception("Baby not found with id $uuid"))

    fun getBabyProgress(uuid: UUID): List<CheckListProgressDto> {
        return listOf(
            CheckListProgressDto(UUID.randomUUID(), UUID.randomUUID(), 1, true))
    }

    @Transactional
    fun deleteBaby(uuid: UUID) {
        val baby = babyRepository.findByUuid(uuid) ?: throw Exception("Baby not found with id $uuid")
        baby.isActive = false
    }

}