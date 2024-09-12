package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Notice
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NoticeRepository : CoroutineCrudRepository<Notice, Long> {
    suspend fun findAllByRemovedFlagIsFalse(): Flow<Notice>

    suspend fun countByRemovedFlagIsFalse(): Long
}
