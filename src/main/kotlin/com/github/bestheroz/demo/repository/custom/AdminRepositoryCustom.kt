package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.entity.Admin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminRepositoryCustom {
    suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Admin>
}
