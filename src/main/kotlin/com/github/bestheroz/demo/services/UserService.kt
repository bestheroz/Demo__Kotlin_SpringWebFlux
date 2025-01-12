package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.user.*
import com.github.bestheroz.demo.repository.UserRepository
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        userRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(UserDto.Response::of)
            .let(ListResult.Companion::of)

    suspend fun getUser(id: Long): UserDto.Response =
        userRepository
            .findById(id)
            ?.apply { operatorHelper.fulfilOperator(this) }
            ?.let(UserDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)

    @Transactional
    suspend fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)?.let {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return request
            .toEntity(operator)
            .apply {
                userRepository.save(this)
                operatorHelper.fulfilOperator(this)
            }.let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val existsDeferred =
            coroutineScope.async(Dispatchers.IO) {
                userRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
            }
        val userDeferred = coroutineScope.async(Dispatchers.IO) { userRepository.findById(id) }

        existsDeferred.await().let {
            if (it) throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return userDeferred
            .await()
            ?.also { if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER) }
            ?.apply {
                update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                userRepository.save(this)
                operatorHelper.fulfilOperator(this)
            }?.let(UserDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    @Transactional
    suspend fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        userRepository
            .findById(id)
            ?.also {
                if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER)
                if (it.id == operator.id) throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF)
            }?.apply {
                remove(operator)
                userRepository.save(this)
            } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response {
        if (operator.id != id && !operator.authorities.contains(AuthorityEnum.USER_EDIT)) {
            throw RequestException400(ExceptionCode.UNKNOWN_AUTHORITY)
        }
        return userRepository
            .findById(id)
            ?.also {
                if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { PasswordUtil.verifyPassword(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }?.apply {
                changePassword(request.newPassword, operator)
                userRepository.save(this)
                operatorHelper.fulfilOperator(this)
            }?.let(UserDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    @Transactional
    suspend fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            ?.also {
                if (!it.useFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { PasswordUtil.verifyPassword(request.password, it) }
                    ?.let {
                        log.warn("password not match")
                        throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                    }
            }?.apply {
                renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                userRepository.save(this)
            }?.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }
            ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        userRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            ?.also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw AuthenticationException401()
                }
            }?.apply {
                if (this.token == refreshToken) {
                    renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                    userRepository.save(this)
                }
            }?.let {
                if (
                    jwtTokenProvider.issuedRefreshTokenIn3Seconds(it.token ?: "") || it.token == refreshToken
                ) {
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "")
                }
                throw AuthenticationException401()
            } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)

    @Transactional
    suspend fun logout(id: Long) {
        userRepository.findById(id)?.apply {
            logout()
            userRepository.save(this)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = !userRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id ?: 0)
}
