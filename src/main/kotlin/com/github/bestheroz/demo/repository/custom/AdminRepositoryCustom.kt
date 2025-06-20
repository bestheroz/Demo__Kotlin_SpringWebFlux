package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.dtos.admin.AdminDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminRepositoryCustom {
    suspend fun findAllWithConditions(
        request: AdminDto.Request,
        pageable: Pageable,
    ): Page<Admin>

    suspend fun existsByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Boolean
}
