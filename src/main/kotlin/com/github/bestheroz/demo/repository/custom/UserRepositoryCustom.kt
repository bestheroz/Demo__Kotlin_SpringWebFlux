package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<User>

    suspend fun existsByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Boolean
}
