package kr.co.lovelydream.global.exception

import jakarta.servlet.http.HttpServletRequest
import kr.co.lovelydream.auth.controller.AuthController
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.vo.ErrorVO
import kr.co.lovelydream.global.vo.ResultVO
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger: Logger = LogManager.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AuthException::class)
    fun handleAuth(e: AuthException, req: HttpServletRequest): ResponseEntity<ErrorVO> {
        val status = when (e.code) {
            ResponseCode.UNAUTHORIZED,
            ResponseCode.AUTH_INVALID_CREDENTIAL,
            ResponseCode.AUTH_INVALID_REFRESH_TOKEN,
            ResponseCode.AUTH_REUSED_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED
            ResponseCode.FORBIDDEN -> HttpStatus.FORBIDDEN
            else -> HttpStatus.BAD_REQUEST
        }
        logger.warn("AUTH ERROR code={} msg={}", e.code, e.message)
        return ResponseEntity.status(status).body(
            ErrorVO(path = req.requestURI, code = e.code.code, message = e.code.message)
        )
    }

    @ExceptionHandler(MemberException::class)
    fun handleMember(e: MemberException, req: HttpServletRequest): ResponseEntity<ErrorVO> {
        val status = when (e.code) {
            ResponseCode.MEMBER_NOT_FOUND -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        logger.warn("MEMBER ERROR code={} msg={}", e.code, e.message)
        return ResponseEntity.status(status).body(
            ErrorVO(path = req.requestURI, code = e.code.code, message = e.code.message)
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidation(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorVO> {
        val msg = when (e) {
            is MethodArgumentNotValidException -> e.bindingResult.fieldErrors.joinToString { "${it.field}:${it.defaultMessage}" }
            is BindException -> e.bindingResult.fieldErrors.joinToString { "${it.field}:${it.defaultMessage}" }
            else -> "Validation error"
        }
        logger.warn("VALIDATION ERROR {}", msg)
        return ResponseEntity.badRequest().body(
            ErrorVO(path = req.requestURI, code = ResponseCode.VALIDATION_ERROR.code, message = msg)
        )
    }

    @ExceptionHandler(
        MissingRequestCookieException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun handleBadRequest(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorVO> {
        logger.warn("BAD REQUEST {}", e.message)
        return ResponseEntity.badRequest().body(
            ErrorVO(path = req.requestURI, code = ResponseCode.FAIL.code, message = e.message ?: "Bad request")
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleEtc(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorVO> {
        logger.error("UNEXPECTED {}", e.message, e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorVO(path = req.requestURI, code = ResponseCode.FAIL.code, message = "서버 처리 실패")
        )
    }

}