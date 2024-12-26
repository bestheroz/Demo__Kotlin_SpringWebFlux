package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.entity.service.OperatorHelper
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query

class UserRepositoryCustomImpl(
    private val template: R2dbcEntityTemplate,
    private val operatorHelper: OperatorHelper,
) : UserRepositoryCustom {
    override suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<User> {
        val query = Query.query(Criteria.where("removed_flag").`is`(false))

        val count = template.count(query, User::class.java).awaitSingle()

        if (count == 0L) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val users =
            template
                .select(User::class.java)
                .matching(query.with(pageable))
                .all()
                .collectList()
                .awaitSingle()
                .let { operatorHelper.fulfilOperator(it) }

        return PageImpl(users, pageable, count)
    }
}
