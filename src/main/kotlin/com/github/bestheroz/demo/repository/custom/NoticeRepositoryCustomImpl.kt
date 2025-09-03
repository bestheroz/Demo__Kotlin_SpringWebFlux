package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.Notice
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query

class NoticeRepositoryCustomImpl(
    private val template: R2dbcEntityTemplate,
    private val operatorHelper: OperatorHelper,
) : NoticeRepositoryCustom {
    override suspend fun findAllWithConditions(
        request: NoticeDto.Request,
        pageable: Pageable,
    ): Page<Notice> =
        coroutineScope {
            val criteria = buildCriteria(request)
            val query = Query.query(criteria).with(pageable)

            // 두 쿼리를 병렬로 실행
            val contentDeferred =
                async {
                    template
                        .select(Notice::class.java)
                        .matching(query)
                        .all()
                        .collectList()
                        .awaitSingle()
                }

            val totalDeferred =
                async {
                    template.count(Query.query(criteria), Notice::class.java).awaitSingle()
                }

            // 두 결과를 모두 기다린 후 처리
            val content = contentDeferred.await().apply { operatorHelper.fulfilOperator(this) }
            val total = totalDeferred.await()

            PageImpl(content, pageable, total)
        }

    private fun buildCriteria(request: NoticeDto.Request): Criteria {
        var criteria = Criteria.where("removed_flag").`is`(false)

        request.id?.let { criteria = criteria.and("id").`is`(it) }

        request.title?.let { criteria = criteria.and("title").like("%$it%") }

        request.useFlag?.let { criteria = criteria.and("use_flag").`is`(it) }

        return criteria
    }
}
