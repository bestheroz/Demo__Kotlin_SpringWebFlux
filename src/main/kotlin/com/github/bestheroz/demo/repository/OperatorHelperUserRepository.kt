package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OperatorHelperUserRepository : CoroutineCrudRepository<User, Long> {
    suspend fun findAllByIdIn(ids: Set<Long>): List<User>
}
