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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> {
        val pageable = PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending())
        return userRepository
            .findAllWithConditions(request, pageable)
            .map(UserDto.Response::of)
            .let(ListResult.Companion::of)
    }

    suspend fun getUser(id: Long): UserDto.Response =
        userRepository
            .findById(id)
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let(UserDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)

    @Transactional
    suspend fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)?.also {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return request
            .toEntity(operator)
            .let {
                userRepository.save(it)
                operatorHelper.fulfilOperator(it)
            }.let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response =
        coroutineScope {
            val existsDeferred =
                async {
                    userRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
                }
            val userDeferred = async { userRepository.findById(id) }

            existsDeferred.await().let {
                if (it) {
                    userDeferred.cancel()
                    throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
                }
            }

            userDeferred
                .await()
                ?.also { if (it.removedFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER) }
                ?.let {
                    it.update(
                        request.loginId,
                        request.password,
                        request.name,
                        request.useFlag,
                        request.authorities,
                        operator,
                    )
                    userRepository.save(it)
                    operatorHelper.fulfilOperator(it)
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
            }?.let {
                it.remove(operator)
                userRepository.save(it)
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
                userRepository.save(it)
                operatorHelper.fulfilOperator(it)
            }?.let(UserDto.Response::of) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    @Transactional
    suspend fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            ?.also {
                if (!it.useFlag) throw RequestException400(ExceptionCode.UNKNOWN_USER)
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(request.password, password) }
                    ?.let {
                        log.warn("password not match")
                        throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                    }
            }?.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                userRepository.save(it)
            }?.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }
            ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto {
        userRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            ?.also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw AuthenticationException401()
                }
            }?.let {
                if (it.token == refreshToken) {
                    it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                    userRepository.save(it)
                }
                it
            }?.let {
                if (
                    jwtTokenProvider.issuedRefreshTokenIn3Seconds(it.token ?: "") || it.token == refreshToken
                ) {
                    return TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "")
                }
                throw AuthenticationException401()
            } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    @Transactional
    suspend fun logout(id: Long) {
        userRepository.findById(id)?.let {
            it.logout()
            userRepository.save(it)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = !userRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id ?: 0)
}
