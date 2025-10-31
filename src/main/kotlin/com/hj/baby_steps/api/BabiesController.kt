package com.hj.baby_steps.api

import com.hj.baby_steps.api.req.AddBabyReq
import com.hj.baby_steps.api.res.GetBabyRes
import com.hj.baby_steps.model.dto.BabyDto
import com.hj.baby_steps.model.dto.CheckListProgressDto
import com.hj.baby_steps.service.BabyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@CrossOrigin(origins = ["http://localhost:5173"])
@RestController
@RequestMapping("/api/babies")
class BabiesController(
    private val babiesService: BabyService
) {

    @PostMapping
    fun addBaby(@RequestBody req: AddBabyReq): ResponseEntity<BabyDto> {
        return ResponseEntity.ok().body(babiesService.addBaby(req.name, req.birthdate))
    }

    @GetMapping
    fun getBabies(): ResponseEntity<List<BabyDto>> {
        return ResponseEntity.ok(babiesService.getBabies())
    }

    @GetMapping("/{id}")
    fun getBaby(@PathVariable id: UUID): ResponseEntity<BabyDto> {
        return ResponseEntity.ok(babiesService.getBaby(id))
    }

    @GetMapping("/{id}/progress")
    fun getBabyProgress(@PathVariable id: UUID): ResponseEntity<List<CheckListProgressDto>> {
        return ResponseEntity.ok(babiesService.getBabyProgress(id))
    }

    @DeleteMapping("/{id}")
    fun deleteBaby(@PathVariable id: UUID): ResponseEntity<Any> {
        babiesService.deleteBaby(id)
        return ResponseEntity.ok().build()
    }
}