package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val operatorHelper: OperatorHelper,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> {
        val pageable = PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending())
        return noticeRepository
            .findAllWithConditions(request, pageable)
            .map(NoticeDto.Response::of)
            .let(ListResult.Companion::of)
    }

    suspend fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository
            .findById(id)
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    @Transactional
    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .let {
                noticeRepository.save(it)
                operatorHelper.fulfilOperator(it)
            }.let(NoticeDto.Response::of)

    @Transactional
    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .findById(id)
            ?.let {
                it.update(request.title, request.content, request.useFlag, operator)
                noticeRepository.save(it)
                operatorHelper.fulfilOperator(it)
            }?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    @Transactional
    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        noticeRepository.findById(id)?.let {
            it.remove(operator)
            noticeRepository.save(it)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
    }
}
