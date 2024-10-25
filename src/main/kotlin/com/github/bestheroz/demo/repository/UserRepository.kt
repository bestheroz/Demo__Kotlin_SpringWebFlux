package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {
    fun findAllByRemovedFlagIsFalse(): Flow<User>

    fun findAllByIdIn(ids: Set<Long>): Flow<User>

    suspend fun countByRemovedFlagIsFalse(): Long

    suspend fun findByLoginIdAndRemovedFlagFalse(loginId: String): User?

    suspend fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): User?
}
