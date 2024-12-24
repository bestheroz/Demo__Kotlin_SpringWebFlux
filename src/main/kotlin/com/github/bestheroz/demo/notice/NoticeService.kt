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

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val operatorHelper: OperatorHelper,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        noticeRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(NoticeDto.Response::of)
            .let { ListResult.of(it) }

    suspend fun getNotice(id: Long): NoticeDto.Response {
        val notice =
            noticeRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
        return operatorHelper.fulfilOperator(notice).let(NoticeDto.Response::of)
    }

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response {
        val entity = request.toEntity(operator)
        println("Saving entity: $entity") // 저장 전 엔티티 출력

        return noticeRepository
            .save(entity)
            .also { println("Saved entity: $it") } // 저장 후 엔티티 출력
            .let { operatorHelper.fulfilOperator(it) }
            .let { NoticeDto.Response.of(it) }
    }

    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response {
        val notice =
            noticeRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
        notice.update(request.title, request.content, request.useFlag, operator)
        return operatorHelper.fulfilOperator(notice).let(NoticeDto.Response::of)
    }

    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository.findById(id)?.remove(operator)
        ?: throw RequestException400(ExceptionCode.UNKNOWN_NOTICE)
}
