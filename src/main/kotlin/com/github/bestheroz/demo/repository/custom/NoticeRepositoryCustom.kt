package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface NoticeRepositoryCustom {
    suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Notice>
}
