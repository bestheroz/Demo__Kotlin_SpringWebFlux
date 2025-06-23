package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.demo.dtos.user.UserDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    suspend fun findAllWithConditions(
        request: UserDto.Request,
        pageable: Pageable,
    ): Page<User>

    suspend fun existsByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Boolean
}
