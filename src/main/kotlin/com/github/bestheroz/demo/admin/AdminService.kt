package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.repository.AdminRepository
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
class AdminService(
    private val adminRepository: AdminRepository,
    private val operatorHelper: OperatorHelper,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> =
        adminRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(AdminDto.Response::of)
            .let { ListResult.of(it) }

    suspend fun getAdmin(id: Long): AdminDto.Response =
        adminRepository
            .findById(id)
            ?.let { operatorHelper.fulfilOperator(it) }
            ?.let { AdminDto.Response.of(it) } ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)

    @Transactional
    suspend fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)?.let {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return request
            .toEntity(operator)
            .let { adminRepository.save(it) }
            .let { operatorHelper.fulfilOperator(it) }
            .let { AdminDto.Response.of(it) }
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val admin =
            adminRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
        admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
        admin
            .takeIf { !request.managerFlag && it.id == operator.id }
            ?.let { throw RequestException400(ExceptionCode.CANNOT_UPDATE_YOURSELF) }
        admin
            .takeIf { !it.managerFlag && !request.managerFlag && !operator.managerFlag }
            ?.let { throw RequestException400(ExceptionCode.UNKNOWN_AUTHORITY) }

        if (adminRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)) {
            throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return admin
            .let {
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                adminRepository.save(it)
            }.let { operatorHelper.fulfilOperator(it) }
            .let { AdminDto.Response.of(it) }
    }

    @Transactional
    suspend fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        val admin =
            adminRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
        admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
        admin
            .takeIf { it.id == operator.id }
            ?.let { throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
        return admin.let {
            it.remove(operator)
            adminRepository.save(it)
        }
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
        val admin =
            adminRepository.findById(id) ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
        admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
        admin.password
            ?.takeUnless { verifyPassword(request.oldPassword, it) }
            ?.let {
                log.warn("password not match")
                throw RequestException400(ExceptionCode.INVALID_PASSWORD)
            }
        admin.password
            ?.takeIf { it == request.newPassword }
            ?.let { throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
        return admin
            .let {
                it.changePassword(request.newPassword, operator)
                adminRepository.save(it)
            }.let { operatorHelper.fulfilOperator(it) }
            .let { AdminDto.Response.of(it) }
    }

    @Transactional
    suspend fun loginAdmin(request: AdminLoginDto.Request): TokenDto {
        val admin =
            adminRepository.findByLoginIdAndRemovedFlagFalse(request.loginId)
                ?: throw RequestException400(ExceptionCode.UNJOINED_ACCOUNT)

        admin
            .takeUnless { admin.useFlag }
            ?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
        admin.password
            ?.takeUnless { verifyPassword(request.password, it) }
            ?.let {
                log.warn("password not match")
                throw RequestException400(ExceptionCode.INVALID_PASSWORD)
            }
        return admin
            .let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                adminRepository.save(it)
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token!!) }
    }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto {
        val admin =
            adminRepository.findById(jwtTokenProvider.getId(refreshToken))
                ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
        admin
            .takeIf {
                admin.removedFlag || admin.token == null || !jwtTokenProvider.validateToken(refreshToken)
            }?.let { throw AuthenticationException401() }

        admin.token?.let { it ->
            if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
            } else if (it == refreshToken) {
                admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
            }
        }
        throw AuthenticationException401()
    }

    @Transactional
    suspend fun logout(id: Long) {
        adminRepository.findById(id)?.let {
            it.logout()
            adminRepository.save(it)
        } ?: throw RequestException400(ExceptionCode.UNKNOWN_ADMIN)
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = !adminRepository.existsByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id)
}
