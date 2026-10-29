package com.hj.baby_steps.api

import com.hj.baby_steps.api.req.AddBabyReq
import com.hj.baby_steps.api.req.UpdateBabyProgressReq
import com.hj.baby_steps.model.dto.BabyDto
import com.hj.baby_steps.model.dto.ChecklistProgressDto
import com.hj.baby_steps.service.BabyService
import com.hj.baby_steps.service.BabyService.Companion.MAX_PHOTO_BYTES
import com.hj.baby_steps.model.User
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/babies")
class BabiesController(
    private val babiesService: BabyService
) {

    @PostMapping
    fun addBaby(@AuthenticationPrincipal user: User, @RequestBody req: AddBabyReq): ResponseEntity<BabyDto> {
        return ResponseEntity.ok().body(babiesService.addBaby(user.id!!, req.name, req.birthdate))
    }

    @GetMapping
    fun getBabies(@AuthenticationPrincipal user: User): ResponseEntity<List<BabyDto>> {
        return ResponseEntity.ok(babiesService.getBabies(user.id!!))
    }

    @GetMapping("/{id}")
    fun getBaby(@AuthenticationPrincipal user: User, @PathVariable(value = "id") babyUUID: UUID): ResponseEntity<BabyDto> {
        return ResponseEntity.ok(babiesService.getBaby(user.id!!, babyUUID))
    }

    @DeleteMapping("/{id}")
    fun deleteBaby(@AuthenticationPrincipal user: User, @PathVariable id: UUID): ResponseEntity<Any> {
        babiesService.deleteBaby(user.id!!, id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/progress")
    fun getBabyProgress(
        @AuthenticationPrincipal user: User,
        @PathVariable(value = "id") babyUUID: UUID
    ): ResponseEntity<List<ChecklistProgressDto>> {
        return ResponseEntity.ok(babiesService.getBabyProgress(user.id!!, babyUUID))
    }

    @PostMapping("/{id}/progress")
    fun updateBabyProgress(
        @AuthenticationPrincipal user: User,
        @PathVariable(value = "id") babyUUID: UUID,
        @RequestBody req: UpdateBabyProgressReq
    ): ResponseEntity<List<ChecklistProgressDto>> {
        return ResponseEntity.ok(babiesService.updateBabyProgress(user.id!!, babyUUID, req.checklistItemId, req.completed))
    }

    @GetMapping("/{id}/photos")
    fun getPhotoMonths(
        @AuthenticationPrincipal user: User,
        @PathVariable id: UUID
    ): ResponseEntity<List<Int>> {
        return ResponseEntity.ok(babiesService.getPhotoMonths(user.id!!, id))
    }

    @GetMapping("/{id}/months/{month}/photo")
    fun getPhoto(
        @AuthenticationPrincipal user: User,
        @PathVariable id: UUID,
        @PathVariable month: Int
    ): ResponseEntity<ByteArray> {
        val photo = babiesService.getPhoto(user.id!!, id, month)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .cacheControl(CacheControl.noCache().cachePrivate())
            .body(photo.data)
    }

    @PutMapping("/{id}/months/{month}/photo")
    fun putPhoto(
        @AuthenticationPrincipal user: User,
        @PathVariable id: UUID,
        @PathVariable month: Int,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        // Read one byte past the limit so an oversized body is rejected without buffering all of it
        val bytes = request.inputStream.readNBytes(MAX_PHOTO_BYTES + 1)
        babiesService.savePhoto(user.id!!, id, month, bytes)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/months/{month}/photo")
    fun deletePhoto(
        @AuthenticationPrincipal user: User,
        @PathVariable id: UUID,
        @PathVariable month: Int
    ): ResponseEntity<Any> {
        babiesService.deletePhoto(user.id!!, id, month)
        return ResponseEntity.noContent().build()
    }

}
