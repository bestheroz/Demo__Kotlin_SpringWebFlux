package com.github.bestheroz.standard.common.domain.service

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.demo.repository.OperatorHelperAdminRepository
import com.github.bestheroz.demo.repository.OperatorHelperUserRepository
import com.github.bestheroz.standard.common.domain.IdCreated
import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class OperatorHelper(
    private val adminRepository: OperatorHelperAdminRepository,
    private val userRepository: OperatorHelperUserRepository,
) {
    suspend fun <T : IdCreatedUpdated> fulfilOperator(operators: List<T>): List<T> =
        coroutineScope {
            if (operators.isEmpty()) return@coroutineScope operators
            val adminIds = HashSet<Long>()
            val userIds = HashSet<Long>()

            collectIds(operators, adminIds, userIds, true)

            val adminMapDeferred = async { fetchAdminMap(adminIds) }
            val userMapDeferred = async { fetchUserMap(userIds) }

            setOperatorData(operators, adminMapDeferred.await(), userMapDeferred.await(), true)

            operators
        }

    suspend fun <T : IdCreatedUpdated> fulfilOperator(operator: T): T = fulfilOperator(listOf(operator)).first()

    suspend fun <T : IdCreated> fulfilCreatedOperator(operators: List<T>): List<T> =
        coroutineScope {
            if (operators.isEmpty()) return@coroutineScope operators
            val adminIds = HashSet<Long>()
            val userIds = HashSet<Long>()

            collectIds(operators, adminIds, userIds, false)

            val adminMapDeferred = async { fetchAdminMap(adminIds) }
            val userMapDeferred = async { fetchUserMap(userIds) }

            setOperatorData(
                operators,
                adminMapDeferred.await(),
                userMapDeferred.await(),
                includeUpdated = false,
            )

            operators
        }

    suspend fun <T : IdCreated> fulfilCreatedOperator(operator: T): T = fulfilCreatedOperator(listOf(operator)).first()

    private fun collectIds(
        operators: List<IdCreated>,
        adminIds: MutableSet<Long>,
        userIds: MutableSet<Long>,
        includeUpdated: Boolean,
    ) {
        for (operator in operators) {
            if (operator.createdObjectType == UserTypeEnum.ADMIN) {
                adminIds.add(checkNotNull(operator.createdObjectId) { "createdObjectId must not be null" })
            } else if (operator.createdObjectType == UserTypeEnum.USER) {
                userIds.add(checkNotNull(operator.createdObjectId) { "createdObjectId must not be null" })
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                if (operator.updatedObjectType == UserTypeEnum.ADMIN) {
                    adminIds.add(
                        checkNotNull(operator.updatedObjectId) { "createdObjectId must not be null" },
                    )
                } else if (operator.updatedObjectType == UserTypeEnum.USER) {
                    userIds.add(checkNotNull(operator.updatedObjectId) { "createdObjectId must not be null" })
                }
            }
        }
    }

    private suspend fun fetchAdminMap(adminIds: Set<Long>): Map<Long, Admin> =
        if (adminIds.isEmpty()) {
            emptyMap()
        } else {
            adminRepository.findAllByIdIn(adminIds).associateBy {
                checkNotNull(it.id) { "Admin ID must not be null" }
            }
        }

    private suspend fun fetchUserMap(userIds: Set<Long>): Map<Long, User> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllByIdIn(userIds).associateBy {
                checkNotNull(it.id) { "User ID must not be null" }
            }
        }

    private fun setOperatorData(
        operators: List<IdCreated>,
        adminMap: Map<Long, Admin>,
        userMap: Map<Long, User>,
        includeUpdated: Boolean,
    ) {
        for (operator in operators) {
            if (operator.createdObjectType == UserTypeEnum.ADMIN) {
                val admin = adminMap[operator.createdObjectId]
                if (admin != null) {
                    operator.createdByAdmin = admin
                }
            } else if (operator.createdObjectType == UserTypeEnum.USER) {
                val user = userMap[operator.createdObjectId]
                if (user != null) {
                    operator.createdByUser = user
                }
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                if (operator.updatedObjectType == UserTypeEnum.ADMIN) {
                    val admin = adminMap[operator.updatedObjectId]
                    if (admin != null) {
                        operator.updatedByAdmin = admin
                    }
                } else if (operator.updatedObjectType == UserTypeEnum.USER) {
                    val user = userMap[operator.updatedObjectId]
                    if (user != null) {
                        operator.updatedByUser = user
                    }
                }
            }
        }
    }
}
