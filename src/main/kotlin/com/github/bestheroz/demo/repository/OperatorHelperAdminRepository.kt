package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Admin
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OperatorHelperAdminRepository : CoroutineCrudRepository<Admin, Long> {
    suspend fun findAllByIdIn(ids: Set<Long>): List<Admin>
}
