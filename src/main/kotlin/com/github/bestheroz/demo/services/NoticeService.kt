package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.security.Operator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        withContext(Dispatchers.IO) {
            val pageable =
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending())
            noticeRepository.findAllWithConditions(request, pageable)
        }.map(NoticeDto.Response::of)
            .let(ListResult.Companion::of)

    @Transactional(readOnly = true)
    suspend fun getNotice(id: Long): NoticeDto.Response =
        withContext(Dispatchers.IO) { noticeRepository.findById(id) }
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        request
            .toEntity(operator)
            .let {
                withContext(Dispatchers.IO) { noticeRepository.save(it) }
                operatorHelper.fulfilOperator(it)
            }.let(NoticeDto.Response::of)

    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        withContext(Dispatchers.IO) { noticeRepository.findById(id) }
            ?.let {
                it.update(request.title, request.content, request.useFlag, operator)
                withContext(Dispatchers.IO) { noticeRepository.save(it) }
                operatorHelper.fulfilOperator(it)
            }?.let(NoticeDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)

    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        noticeRepository.findById(id)?.let {
            it.remove(operator)
            withContext(Dispatchers.IO) { noticeRepository.save(it) }
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
    }
}
