package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.admin.AdminChangePasswordDto
import com.github.bestheroz.demo.dtos.admin.AdminCreateDto
import com.github.bestheroz.demo.dtos.admin.AdminDto
import com.github.bestheroz.demo.dtos.admin.AdminLoginDto
import com.github.bestheroz.demo.dtos.admin.AdminUpdateDto
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.domain.service.OperatorHelper
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val adminRepository: AdminRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> =
        withContext(Dispatchers.IO) {
            val pageable =
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending())
            adminRepository.findAllWithConditions(request, pageable)
        }.map(AdminDto.Response::of)
            .let(ListResult.Companion::of)

    suspend fun getAdmin(id: Long): AdminDto.Response =
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let(AdminDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    suspend fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)
        }?.also { throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return request
            .toEntity(operator)
            .let {
                withContext(Dispatchers.IO) { adminRepository.save(it) }
                operatorHelper.fulfilOperator(it)
            }.let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val existsDeferred =
            coroutineScope.async(Dispatchers.IO) {
                adminRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
            }
        val userDeferred = coroutineScope.async(Dispatchers.IO) { adminRepository.findById(id) }

        existsDeferred.await().let {
            if (it) {
                userDeferred.cancel()
                throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
            }
        }

        return userDeferred
            .await()
            ?.also {
                if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
                if (!request.managerFlag && it.id == operator.id) {
                    throw RequestException400(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                }
                if (!it.managerFlag && !request.managerFlag && !operator.managerFlag) {
                    throw RequestException400(ExceptionCode.UNKNOWN_AUTHORITY)
                }
            }?.let {
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                withContext(Dispatchers.IO) { adminRepository.save(it) }
                operatorHelper.fulfilOperator(it)
            }?.let(AdminDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    @Transactional
    suspend fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            ?.also {
                if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
                if (it.id == operator.id) throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF)
            }?.let {
                it.remove(operator)
                withContext(Dispatchers.IO) { adminRepository.save(it) }
            } ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        if (operator.id != id && !operator.authorities.contains(AuthorityEnum.ADMIN_EDIT)) {
            throw RequestException400(ExceptionCode.UNKNOWN_AUTHORITY)
        }
        return withContext(Dispatchers.IO) { adminRepository.findById(id) }
            ?.also {
                if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(request.oldPassword, password) }
                    ?.let {
                        log.warn("password not match")
                        throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> PasswordUtil.isPasswordValid(request.newPassword, password) }
                    ?.let { throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }?.let {
                it.changePassword(request.newPassword, operator)
                withContext(Dispatchers.IO) { adminRepository.save(it) }
                operatorHelper.fulfilOperator(it)
            }?.let(AdminDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    @Transactional
    suspend fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)
        }?.also {
            if (!it.useFlag) throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
            it.password
                ?.takeUnless { PasswordUtil.isPasswordValid(request.password, it) }
                ?.let {
                    log.warn("password not match")
                    throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                }
        }?.let {
            it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
            withContext(Dispatchers.IO) { adminRepository.save(it) }
        }?.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }
            ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto {
        return withContext(Dispatchers.IO) {
            adminRepository.findById(jwtTokenProvider.getId(refreshToken))
        }?.also {
            if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                throw AuthenticationException401()
            }
        }?.let {
            if (it.token == refreshToken) {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                withContext(Dispatchers.IO) { adminRepository.save(it) }
            }
            it
        }?.let {
            if (
                jwtTokenProvider.issuedRefreshTokenIn3Seconds(it.token ?: "") || it.token == refreshToken
            ) {
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "")
            }
            throw AuthenticationException401()
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    @Transactional
    suspend fun logout(id: Long) {
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            ?.let {
                it.logout()
                withContext(Dispatchers.IO) { adminRepository.save(it) }
            } ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        !withContext(Dispatchers.IO) {
            adminRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id)
        }
}
