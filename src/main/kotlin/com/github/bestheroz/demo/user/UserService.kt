package com.github.bestheroz.demo.user

import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.entity.service.OperatorHelper
import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    @Transactional(readOnly = true)
    suspend fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        ListResult(
            page = request.page,
            pageSize = request.pageSize,
            total = userRepository.countByRemovedFlagIsFalse(),
            items =
                operatorHelper
                    .fulfilOperator(
                        userRepository
                            .findAllByRemovedFlagIsFalse()
                            .drop(request.page * request.pageSize) // 페이징 시작점
                            .take(request.pageSize)
                            .toList(),
                    ).map(UserDto.Response::of),
        )

    @Transactional(readOnly = true)
    suspend fun getUser(id: Long): UserDto.Response {
        val user = userRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        return UserDto.Response.of(operatorHelper.fulfilOperator(user))
    }

    suspend fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)?.let {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return UserDto.Response.of(operatorHelper.fulfilOperator(userRepository.save(request.toEntity(operator))))
    }

    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val user =
            userRepository
                .findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }

        userRepository
            .findByLoginIdAndRemovedFlagFalseAndIdNot(
                request.loginId,
                id,
            )?.let { throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT) }

        user.update(
            request.loginId,
            request.password,
            request.name,
            request.useFlag,
            request.authorities,
            operator,
        )
        return UserDto.Response.of(operatorHelper.fulfilOperator(user))
    }

    suspend fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        val user =
            userRepository
                .findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user
            .takeIf { it.id == operator.id }
            ?.let { throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
        user.remove(operator)
    }

    suspend fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val user =
            userRepository
                .findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user.password?.takeUnless { verifyPassword(request.oldPassword, it) }?.let {
            log.warn("password not match")
            throw RequestException400(ExceptionCode.INVALID_PASSWORD)
        }
        user.password?.takeIf { it == request.newPassword }?.let {
            throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD)
        }
        user.changePassword(request.newPassword, operator)
        return UserDto.Response.of(operatorHelper.fulfilOperator(user))
    }

    suspend fun loginUser(request: UserLoginDto.Request): TokenDto {
        val user =
            userRepository
                .findByLoginIdAndRemovedFlagFalse(request.loginId) ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)
        user
            .takeIf { it.removedFlag || !user.useFlag }
            ?.let { throw RequestException400(ExceptionCode.UNKNOWN_USER) }
        user.password?.takeUnless { verifyPassword(request.password, it) }?.let {
            log.warn("password not match")
            throw RequestException400(ExceptionCode.INVALID_PASSWORD)
        }
        user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
        return TokenDto(
            jwtTokenProvider.createAccessToken(Operator(user)),
            user.token!!,
        )
    }

    suspend fun renewToken(refreshToken: String): TokenDto {
        val user =
            userRepository
                .findById(jwtTokenProvider.getId(refreshToken))
                ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user
            .takeIf { user.removedFlag || user.token == null || !jwtTokenProvider.validateToken(refreshToken) }
            ?.let { throw AuthenticationException401() }
        user.token?.let {
            if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                return TokenDto(
                    jwtTokenProvider.createAccessToken(Operator(user)),
                    it,
                )
            } else if (it == refreshToken) {
                user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
                return TokenDto(
                    jwtTokenProvider.createAccessToken(Operator(user)),
                    it,
                )
            }
        }
        throw AuthenticationException401()
    }

    suspend fun logout(id: Long) {
        val user =
            userRepository
                .findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_USER)
        user.logout()
    }

    @Transactional(readOnly = true)
    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id) == null
}
