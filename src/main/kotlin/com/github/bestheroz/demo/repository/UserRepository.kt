package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {
    fun findAllByRemovedFlagIsFalse(): Flow<User>
    fun countByRemovedFlagIsFalse(): Long

    fun findByLoginIdAndRemovedFlagFalse(loginId: String): User?

    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): User?
}
