package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import java.time.Instant

abstract class IdCreated {
    @Id var id: Long? = null

    lateinit var createdAt: Instant

    lateinit var createdObjectType: UserTypeEnum

    var createdObjectId: Long? = null

    @Transient var createdByAdmin: Admin? = null

    @Transient var createdByUser: User? = null

    @Transient var creator: Operator? = null

    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
        creator = operator
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                createdByAdmin = Admin.of(operator)
                createdByUser = null
            }
            UserTypeEnum.USER -> {
                createdByAdmin = null
                createdByUser = User.of(operator)
            }
        }
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN ->
                    creator?.let(UserSimpleDto::of)
                        ?: createdByAdmin?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByAdmin nor creator exists")
                UserTypeEnum.USER ->
                    creator?.let(UserSimpleDto::of)
                        ?: createdByUser?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByUser nor creator exists")
            }
}
