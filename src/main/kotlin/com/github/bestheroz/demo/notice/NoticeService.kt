package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.entity.service.OperatorHelper
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
        noticeRepository.findById(id)?.let {
            operatorHelper.fulfilOperator(it)
            NoticeDto.Response.of(it)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .let { noticeRepository.save(it) }
            .let { operatorHelper.fulfilOperator(it) }
            .let { NoticeDto.Response.of(it) }

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
            }?.let { operatorHelper.fulfilOperator(it) }
            ?.let { NoticeDto.Response.of(it) } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository.findById(id)?.let {
        it.remove(operator)
        noticeRepository.save(it)
    } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
}
