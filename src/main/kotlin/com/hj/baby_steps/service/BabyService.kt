package com.hj.baby_steps.service

//import com.hj.baby_steps.repository.BabiesRepository
import com.hj.baby_steps.model.Baby
import com.hj.baby_steps.model.dto.BabyDto
import com.hj.baby_steps.model.dto.ChecklistProgressDto
import com.hj.baby_steps.repository.BabyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class BabyService(
    val babyRepository: BabyRepository
) {

    @Transactional
    fun addBaby(name: String, birthdate: LocalDate): BabyDto {
        return BabyDto.from(babyRepository.save(Baby.create(name, birthdate)))
    }

    fun getBabies(): List<BabyDto> =
        babyRepository.findByIsActiveIsTrue().map { BabyDto.from(it) }

    fun getBaby(babyUUID: UUID): BabyDto =
        BabyDto.from(babyRepository.findByUuid(babyUUID) ?: throw Exception("Baby not found with id $babyUUID"))

    @Transactional
    fun deleteBaby(uuid: UUID) {
        val baby = babyRepository.findByUuid(uuid) ?: throw Exception("Baby not found with id $uuid")
        baby.isActive = false
    }

    fun getBabyProgress(babyUUID: UUID): List<ChecklistProgressDto> {
        val baby = babyRepository.findByUuid(babyUUID) ?: throw Exception("Baby not found with id $babyUUID")
        return ChecklistProgressDto.to(baby.checklistProgress!!.uuid, baby.uuid, baby.checklistProgress!!.checklistItemId)
    }

    @Transactional
    fun updateBabyProgress(
        babyUUID: UUID,
        checklistItemId: String,
        completed: Boolean
    ): List<ChecklistProgressDto> {
        var baby = babyRepository.findByUuid(babyUUID) ?: throw Exception("Baby not found with id $babyUUID")
        baby.updateCheckList(checklistItemId, completed)
        baby = babyRepository.save(baby)
        return ChecklistProgressDto.to(
            baby.checklistProgress!!.uuid, baby.uuid, baby.checklistProgress!!.checklistItemId)
    }
}