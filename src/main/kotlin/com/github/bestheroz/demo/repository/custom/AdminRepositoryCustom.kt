package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.Admin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminRepositoryCustom {
    suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Admin>

    suspend fun existsByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Boolean
}
