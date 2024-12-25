package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.entity.service.OperatorHelper
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryCustomImpl(
    private val template: R2dbcEntityTemplate,
    private val operatorHelper: OperatorHelper,
) : UserRepositoryCustom {
    override suspend fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<User> {
        val offset = pageable.offset
        val limit = pageable.pageSize

        val count =
            template
                .count(Query.query(Criteria.where("removed_flag").`is`(false)), User::class.java)
                .awaitSingle()

        if (count == 0L) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val users =
            template
                .select(User::class.java)
                .matching(
                    Query.query(Criteria.where("removed_flag").`is`(false)).limit(limit).offset(offset),
                ).all()
                .collectList()
                .awaitFirst()

        return PageImpl(operatorHelper.fulfilOperator(users), pageable, count)
    }
}
