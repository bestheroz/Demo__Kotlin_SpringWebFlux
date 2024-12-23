package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Notice
import com.github.bestheroz.demo.repository.custom.NoticeRepositoryCustom
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NoticeRepository :
    CoroutineCrudRepository<Notice, Long>,
    NoticeRepositoryCustom
