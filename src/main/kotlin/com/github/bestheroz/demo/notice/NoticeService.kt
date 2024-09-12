package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.security.Operator
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> = ListResult(
        page = request.page,
        pageSize = request.pageSize,
        total = noticeRepository.countByRemovedFlagIsFalse(),
        items = noticeRepository.findAllByRemovedFlagIsFalse(
        ).drop(request.page * request.pageSize)  // 페이징 시작점
            .take(request.pageSize).toList().map(NoticeDto.Response::of),
    )

    suspend fun getNotice(id: Long): NoticeDto.Response {
        val notice = noticeRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
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
        val notice = noticeRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
        notice.update(request.title, request.content, request.useFlag, operator)
        return NoticeDto.Response.of(notice)
    }

    suspend fun deleteNotice(id: Long, operator: Operator) {
        val notice = noticeRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
        notice.remove(operator)
    }
}
