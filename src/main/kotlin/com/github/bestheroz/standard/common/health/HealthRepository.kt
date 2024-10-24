package com.github.bestheroz.standard.common.health

import com.github.bestheroz.demo.entity.Admin
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HealthRepository : CoroutineCrudRepository<Admin, Long?> {
    @Query(value = "select now()")
    fun selectNow()
}
