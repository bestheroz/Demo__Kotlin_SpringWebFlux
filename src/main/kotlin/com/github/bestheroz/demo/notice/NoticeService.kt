package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> {
        val result = noticeRepository.findAllByRemovedFlagIsFalse(
            PageRequest.of(
                request.page - 1,
                request.pageSize,
                Sort.by("id").descending()
            )
        ).map(NoticeDto.Response::of)
        return ListResult.of(result)
    }

    suspend fun getNotice(id: Long): NoticeDto.Response {
        val notice = noticeRepository.findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }
        return NoticeDto.Response.of(notice)
    }

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator
    ): NoticeDto.Response {
        val entity = request.toEntity(operator)
        val savedNotice = noticeRepository.save(entity)
        return NoticeDto.Response.of(savedNotice)
    }

    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator
    ): NoticeDto.Response {
        val notice = noticeRepository.findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }
        notice.update(request.title, request.content, request.useFlag, operator)
        return NoticeDto.Response.of(notice)
    }

    suspend fun deleteNotice(id: Long, operator: Operator) {
        val notice = noticeRepository.findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }
        notice.remove(operator)
    }
}
