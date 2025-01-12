package com.github.bestheroz.demo.dtos.notice

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
@Transactional
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val operatorHelper: OperatorHelper,
) {
    @Transactional(readOnly = true)
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        noticeRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(NoticeDto.Response::of)
            .let { ListResult.of(it) }

    @Transactional(readOnly = true)
    suspend fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository
            .findById(id)
            ?.apply { operatorHelper.fulfilOperator(this) }
            ?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .apply {
                noticeRepository.save(this)
                operatorHelper.fulfilOperator(this)
            }.let(NoticeDto.Response::of)

    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .findById(id)
            ?.apply {
                update(request.title, request.content, request.useFlag, operator)
                noticeRepository.save(this)
                operatorHelper.fulfilOperator(this)
            }?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        noticeRepository.findById(id)?.apply {
            remove(operator)
            noticeRepository.save(this)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
    }
}
