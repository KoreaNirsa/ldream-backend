package kr.co.lovelydream.global.exception

import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.vo.ResultVO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuthException::class)
    fun handleAuthException(ex: AuthException): ResponseEntity<ResultVO<Nothing>> {
        val code = ex.code
        val status = when (code) {
            ResponseCode.AUTH_CODE_EXPIRED,
            ResponseCode.AUTH_CODE_MISMATCH,
            ResponseCode.AUTH_INVALID_CREDENTIAL,
            ResponseCode.AUTH_UNAUTHORIZED
                -> HttpStatus.FORBIDDEN

            ResponseCode.AUTH_EMAIL_ALREADY_EXISTS
                -> HttpStatus.CONFLICT

            else -> HttpStatus.BAD_REQUEST
        }

        return ResponseEntity.status(status)
            .body(ResultVO(code.code, code.message, null))
    }

}