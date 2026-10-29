package com.hj.baby_steps.service

import com.hj.baby_steps.model.Baby
import com.hj.baby_steps.model.BabyMonthPhoto
import com.hj.baby_steps.model.dto.BabyDto
import com.hj.baby_steps.model.dto.ChecklistProgressDto
import com.hj.baby_steps.repository.BabyMonthPhotoRepository
import com.hj.baby_steps.repository.BabyRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Every operation is scoped to the owning user. A baby that belongs to someone else is reported
 * as not found, so one account can't probe for another account's baby IDs.
 */
@Service
class BabyService(
    val babyRepository: BabyRepository,
    val photoRepository: BabyMonthPhotoRepository
) {

    @Transactional
    fun addBaby(ownerId: Long, name: String, birthdate: LocalDate): BabyDto {
        if (name.isBlank() || name.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1-100 characters")
        }
        return BabyDto.from(babyRepository.save(Baby.create(name.trim(), birthdate, ownerId)))
    }

    fun getBabies(ownerId: Long): List<BabyDto> =
        babyRepository.findByOwnerIdAndIsActiveIsTrue(ownerId).map { BabyDto.from(it) }

    fun getBaby(ownerId: Long, babyUUID: UUID): BabyDto =
        BabyDto.from(findOwned(ownerId, babyUUID))

    /** Hard delete: the baby row and its checklist progress (cascaded) are removed, not just hidden. */
    @Transactional
    fun deleteBaby(ownerId: Long, uuid: UUID) {
        babyRepository.delete(findOwned(ownerId, uuid))
    }

    fun getBabyProgress(ownerId: Long, babyUUID: UUID): List<ChecklistProgressDto> {
        val baby = findOwned(ownerId, babyUUID)
        return ChecklistProgressDto.to(baby.checklistProgress!!.uuid, baby.uuid, baby.checklistProgress!!.checklistItemId)
    }

    @Transactional
    fun updateBabyProgress(
        ownerId: Long,
        babyUUID: UUID,
        checklistItemId: String,
        completed: Boolean
    ): List<ChecklistProgressDto> {
        var baby = findOwned(ownerId, babyUUID)
        baby.updateCheckList(checklistItemId, completed)
        baby = babyRepository.save(baby)
        return ChecklistProgressDto.to(
            baby.checklistProgress!!.uuid, baby.uuid, baby.checklistProgress!!.checklistItemId)
    }

    fun getPhotoMonths(ownerId: Long, babyUUID: UUID): List<Int> =
        photoRepository.findMonthsByBabyId(findOwned(ownerId, babyUUID).id!!)

    fun getPhoto(ownerId: Long, babyUUID: UUID, month: Int): BabyMonthPhoto {
        checkMonth(month)
        return photoRepository.findByBabyIdAndMonth(findOwned(ownerId, babyUUID).id!!, month)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No photo for month $month")
    }

    /** Stores or replaces the photo for a month. The type comes from the bytes, not from the client's header. */
    @Transactional
    fun savePhoto(ownerId: Long, babyUUID: UUID, month: Int, bytes: ByteArray) {
        checkMonth(month)
        if (bytes.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty photo")
        if (bytes.size > MAX_PHOTO_BYTES) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo over 2 MB")
        val contentType = sniffImageType(bytes)
            ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG or WebP photos")
        val babyId = findOwned(ownerId, babyUUID).id!!
        val photo = photoRepository.findByBabyIdAndMonth(babyId, month)
        if (photo == null) {
            photoRepository.save(BabyMonthPhoto(babyId = babyId, month = month, contentType = contentType, data = bytes))
        } else {
            photo.contentType = contentType
            photo.data = bytes
            photo.updatedOn = Instant.now()
        }
    }

    @Transactional
    fun deletePhoto(ownerId: Long, babyUUID: UUID, month: Int) {
        checkMonth(month)
        photoRepository.deleteByBabyIdAndMonth(findOwned(ownerId, babyUUID).id!!, month)
    }

    private fun checkMonth(month: Int) {
        if (month !in 0..24) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be 0-24")
    }

    private fun sniffImageType(b: ByteArray): String? = when {
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "image/jpeg"
        b.size >= 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() &&
            b[3] == 'G'.code.toByte() -> "image/png"
        b.size >= 12 && String(b, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> null
    }

    private fun findOwned(ownerId: Long, uuid: UUID): Baby =
        babyRepository.findByUuidAndOwnerIdAndIsActiveIsTrue(uuid, ownerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Baby not found with id $uuid")

    companion object {
        const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
    }
}
