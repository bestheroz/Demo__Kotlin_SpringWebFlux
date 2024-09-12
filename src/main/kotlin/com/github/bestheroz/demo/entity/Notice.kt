package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("notice")
data class Notice(
    @Column var title: String,
    @Column var content: String,
    @Column var useFlag: Boolean,
    @Column var removedFlag: Boolean = false,
    private var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    companion object {
        fun of(
            title: String,
            content: String,
            useFlag: Boolean,
            operator: Operator,
        ) = Notice(
            title = title,
            content = content,
            useFlag = useFlag,
        ).apply {
            val now = Instant.now()
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }
    }

    fun update(
        title: String,
        content: String,
        useFlag: Boolean,
        operator: Operator,
    ) {
        this.title = title
        this.content = content
        this.useFlag = useFlag
        val now = Instant.now()
        this.setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        this.removedFlag = true
        val now = Instant.now()
        this.removedAt = now
        this.setUpdatedBy(operator, now)
    }
}
