package com.github.bestheroz.demo.user

import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.entity.service.OperatorHelper
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        userRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(UserDto.Response::of)
            .let { ListResult.of(it) }

    suspend fun getUser(id: Long): UserDto.Response =
        userRepository
            .findById(id)
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let { UserDto.Response.of(it) } ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)

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
            .let { userRepository.save(it) }
            .let { operatorHelper.fulfilOperator(it) }
            .let { UserDto.Response.of(it) }
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val user = userRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }

        if (userRepository.countByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id) > 0) {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return user
            .let {
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                userRepository.save(it)
            }.let { operatorHelper.fulfilOperator(it) }
            .let { UserDto.Response.of(it) }
    }

    @Transactional
    suspend fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        val user = userRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user
            .takeIf { it.id == operator.id }
            ?.let { throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
        return user.let {
            it.remove(operator)
            userRepository.save(it)
        }
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
        val user = userRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user.password
            ?.takeUnless { verifyPassword(request.oldPassword, it) }
            ?.let {
                log.warn("password not match")
                throw RequestException400(ExceptionCode.INVALID_PASSWORD)
            }
        user.password
            ?.takeIf { it == request.newPassword }
            ?.let { throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
        return user
            .let {
                it.changePassword(request.newPassword, operator)
                userRepository.save(it)
            }.let { operatorHelper.fulfilOperator(it) }
            .let { UserDto.Response.of(it) }
    }

    @Transactional
    suspend fun loginUser(request: UserLoginDto.Request): TokenDto {
        val user =
            userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)
                ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)
        user
            .takeIf { it.removedFlag || !user.useFlag }
            ?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user.password
            ?.takeUnless { verifyPassword(request.password, it) }
            ?.let {
                log.warn("password not match")
                throw RequestException400(ExceptionCode.INVALID_PASSWORD)
            }
        return user
            .let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                userRepository.save(it)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token!!) }
    }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto {
        val user =
            userRepository.findById(jwtTokenProvider.getId(refreshToken))
                ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user
            .takeIf {
                user.removedFlag || user.token == null || !jwtTokenProvider.validateToken(refreshToken)
            }?.let { throw AuthenticationException401() }
        user.token?.let {
            if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
            } else if (it == refreshToken) {
                user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
            }
        }
        throw AuthenticationException401()
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
    ): Boolean = userRepository.countByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id ?: 0) == 0L
}
