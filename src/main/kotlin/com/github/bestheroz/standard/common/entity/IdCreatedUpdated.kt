package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import java.time.Instant

abstract class IdCreatedUpdated : IdCreated() {
    @Column("updated_object_type")
    lateinit var updatedObjectType: UserTypeEnum

    @Column("updated_at")
    lateinit var updatedAt: Instant

    @Column("updated_object_id")
    var updatedObjectId: Long? = null

    @Transient
    var updatedByAdmin: Admin? = null

    @Transient
    var updatedByUser: User? = null

    fun setUpdatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                updatedObjectType = UserTypeEnum.ADMIN
                updatedByAdmin = Admin.of(operator)
            }
            UserTypeEnum.USER -> {
                updatedObjectType = UserTypeEnum.USER
                updatedByUser = User.of(operator)
            }
        }
        updatedAt = instant
        updatedObjectId = operator.id
        updatedObjectType = operator.type
    }

    val updatedBy: UserSimpleDto
        get() =
            when (updatedObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.of(updatedByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.of(updatedByUser!!)
            }
}
