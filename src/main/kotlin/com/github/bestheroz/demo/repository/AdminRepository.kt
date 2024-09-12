package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Admin
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminRepository : CoroutineCrudRepository<Admin, Long> {
    fun findAllByRemovedFlagIsFalse(): Flow<Admin>
    fun countByRemovedFlagIsFalse(): Long

    fun findByLoginIdAndRemovedFlagFalse(loginId: String): Admin?

    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Admin?
}
