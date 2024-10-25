package com.github.bestheroz.standard.common.entity.service

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.entity.IdCreated
import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class OperatorHelper(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
) {
    suspend fun <T : IdCreatedUpdated> fulfilOperator(operators: List<T>): List<T> {
        val adminIds = HashSet<Long>()
        val userIds = HashSet<Long>()

        collectIds(operators, adminIds, userIds, true)

        val adminMap = fetchAdminMap(adminIds)
        val userMap = fetchUserMap(userIds)

        setOperatorData(operators, adminMap, userMap, true)

        return operators
    }

    suspend fun <T : IdCreatedUpdated> fulfilOperator(operator: T): T = fulfilOperator(listOf(operator)).first()

    suspend fun <T : IdCreated> fulfilCreatedOperator(operators: List<T>): List<T> {
        val adminIds = HashSet<Long>()
        val userIds = HashSet<Long>()

        collectIds(operators, adminIds, userIds, false)

        val adminMap = fetchAdminMap(adminIds)
        val userMap = fetchUserMap(userIds)

        setOperatorData(operators, adminMap, userMap, false)

        return operators
    }

    suspend fun <T : IdCreated> fulfilCreatedOperator(operator: T): T = fulfilCreatedOperator(listOf(operator)).first()

    private fun collectIds(
        operators: List<out IdCreated>,
        adminIds: MutableSet<Long>,
        userIds: MutableSet<Long>,
        includeUpdated: Boolean,
    ) {
        for (operator in operators) {
            if (operator.createdObjectType == UserTypeEnum.ADMIN) {
                adminIds.add(operator.createdObjectId!!)
            } else if (operator.createdObjectType == UserTypeEnum.USER) {
                userIds.add(operator.createdObjectId!!)
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                if (operator.updatedObjectType == UserTypeEnum.ADMIN) {
                    adminIds.add(operator.updatedObjectId!!)
                } else if (operator.updatedObjectType == UserTypeEnum.USER) {
                    userIds.add(operator.updatedObjectId!!)
                }
            }
        }
    }

    private suspend fun fetchAdminMap(adminIds: Set<Long>): Map<Long, Admin> =
        if (adminIds.isEmpty()) {
            emptyMap()
        } else {
            adminRepository
                .findAllByIdIn(adminIds)
                .toList()
                .associateBy { it.id!! }
        }

    private suspend fun fetchUserMap(userIds: Set<Long>): Map<Long, User> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository
                .findAllByIdIn(userIds)
                .toList()
                .associateBy { it.id!! }
        }

    private fun setOperatorData(
        operators: List<out IdCreated>,
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
