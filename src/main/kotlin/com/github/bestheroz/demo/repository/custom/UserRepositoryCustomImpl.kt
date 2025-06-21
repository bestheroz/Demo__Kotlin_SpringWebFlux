package com.github.bestheroz.demo.repository.custom

import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
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
    override suspend fun findAllWithConditions(
        request: UserDto.Request,
        pageable: Pageable,
    ): Page<User> {
        val criteria = buildCriteria(request)
        val query = Query.query(criteria).with(pageable)
        val content =
            template.select(User::class.java).matching(query).all().collectList().awaitSingle().apply {
                operatorHelper.fulfilOperator(this)
            }
        val total = template.count(Query.query(criteria), User::class.java).awaitSingle()
        return PageImpl(content, pageable, total)
    }

    private fun buildCriteria(request: UserDto.Request): Criteria {
        var criteria = Criteria.where("removed_flag").`is`(false)
        request.id?.let { criteria = criteria.and("id").`is`(it) }
        request.loginId?.let { criteria = criteria.and("login_id").like("%$it%") }
        request.name?.let { criteria = criteria.and("name").like("%$it%") }
        return criteria
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
            .exists(Query.query(id?.let { criteria.and("id").not(it) } ?: criteria), User::class.java)
            .awaitSingle()
    }
}
