package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Admin
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminRepository : CoroutineCrudRepository<Admin, Long> {
    fun findAllByRemovedFlagIsFalse(): Flow<Admin>

    fun findAllByIdIn(ids: Set<Long>): Flow<Admin>

    suspend fun countByRemovedFlagIsFalse(): Long

    suspend fun findByLoginIdAndRemovedFlagFalse(loginId: String): Admin?

    suspend fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Admin?
}
