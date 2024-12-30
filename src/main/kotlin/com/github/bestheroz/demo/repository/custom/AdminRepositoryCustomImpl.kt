package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query

class AdminRepositoryCustomImpl(
    private val template: R2dbcEntityTemplate,
    private val operatorHelper: OperatorHelper,
) : AdminRepositoryCustom {
    override suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Admin> {
        val query = Query.query(Criteria.where("removed_flag").`is`(false))

        val count = template.count(query, Admin::class.java).awaitSingle()

        if (count == 0L) {
            return PageImpl(emptyList(), pageable, 0)
        }

        return template
            .select(Admin::class.java)
            .matching(query.with(pageable))
            .all()
            .collectList()
            .awaitSingle()
            .let { operatorHelper.fulfilOperator(it) }
            .let { PageImpl(it, pageable, count) }
    }

    override suspend fun existsByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Boolean {
        val criteria =
            Criteria
                .where("login_id")
                .`is`(loginId)
                .and("removed_flag")
                .`is`(false)

        return template
            .exists(Query.query(id?.let { criteria.and("id").not(it) } ?: criteria), Admin::class.java)
            .awaitSingle()
    }
}
