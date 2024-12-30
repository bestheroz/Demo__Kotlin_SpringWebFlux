package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.repository.custom.AdminRepositoryCustom
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminRepository :
    CoroutineCrudRepository<Admin, Long>,
    AdminRepositoryCustom {
    suspend fun findByLoginIdAndRemovedFlagFalse(loginId: String): Admin?
}
