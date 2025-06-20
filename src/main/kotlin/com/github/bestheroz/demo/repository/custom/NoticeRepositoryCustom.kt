package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.Notice
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface NoticeRepositoryCustom {
    suspend fun findAllWithConditions(
        request: NoticeDto.Request,
        pageable: Pageable,
    ): Page<Notice>
}
