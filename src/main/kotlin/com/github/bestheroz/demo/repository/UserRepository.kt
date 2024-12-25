package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.demo.repository.custom.UserRepositoryCustom
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository :
    CoroutineCrudRepository<User, Long>,
    UserRepositoryCustom {
    suspend fun findByLoginIdAndRemovedFlagFalse(loginId: String): User?

    suspend fun countByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long,
    ): Long
}
