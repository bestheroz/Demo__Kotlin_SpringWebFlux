package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("admin")
data class Admin(
    @Column("login_id")
    var loginId: String,
    var password: String? = null,
    var token: String? = null,
    var name: String,
    @Column("use_flag")
    var useFlag: Boolean,
    @Column("manager_flag")
    var managerFlag: Boolean,
    @Column("authorities")
    var _authorities: List<AuthorityEnum>,
    @Column("change_password_at")
    var changePasswordAt: Instant? = null,
    @Column("latest_active_at")
    var latestActiveAt: Instant? = null,
    @Column("joined_at")
    var joinedAt: Instant? = null,
    @Column("removed_flag")
    var removedFlag: Boolean = false,
    @Column("removed_at")
    var removedAt: Instant? = null,
    // Inherit from the converted IdCreatedUpdated
) : IdCreatedUpdated() {
    fun getType(): UserTypeEnum = UserTypeEnum.ADMIN

    var authorities: List<AuthorityEnum>
        get() = if (managerFlag) AuthorityEnum.entries else _authorities
        set(value) {
            _authorities = value
        }

    companion object {
        fun of(
            loginId: String,
            password: String,
            name: String,
            useFlag: Boolean,
            managerFlag: Boolean,
            authorities: List<AuthorityEnum>,
            operator: Operator,
        ) = Admin(
            loginId = loginId,
            name = name,
            useFlag = useFlag,
            managerFlag = managerFlag,
            _authorities = authorities,
        ).apply {
            this.password = getPasswordHash(password)
            val now = Instant.now()
            this.joinedAt = now
            this.removedFlag = false
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }

        fun of(operator: Operator) =
            Admin(
                loginId = operator.loginId,
                name = operator.name,
                useFlag = false,
                managerFlag = operator.managerFlag,
                _authorities = emptyList(),
            ).apply {
                this.id = operator.id
            }
    }

    fun update(
        loginId: String,
        password: String?,
        name: String,
        useFlag: Boolean,
        managerFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        this.loginId = loginId
        this.name = name
        this.useFlag = useFlag
        this.managerFlag = managerFlag
        this.authorities = authorities
        val now = Instant.now()
        setUpdatedBy(operator, now)
        password?.let {
            this.password = getPasswordHash(password)
            this.changePasswordAt = now
        }
    }

    fun changePassword(
        password: String,
        operator: Operator,
    ) {
        this.password = getPasswordHash(password)
        val now = Instant.now()
        this.changePasswordAt = now
        setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        removedFlag = true
        val now = Instant.now()
        removedAt = now
        setUpdatedBy(operator, now)
    }

    fun renewToken(token: String) {
        this.token = token
        latestActiveAt = Instant.now()
    }

    fun logout() {
        token = null
    }
}
