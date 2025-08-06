package kr.co.lovelydream.global.response

import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.vo.ResultVO
import org.springframework.http.ResponseEntity

object ResultResponse {

    fun <T> success(data: T?, code: ResponseCode = ResponseCode.SUCCESS): ResponseEntity<ResultVO<T>> {
        return ResponseEntity.ok(ResultVO(code.code, code.message, data))
    }

    fun error(code: ResponseCode = ResponseCode.VALIDATION_ERROR): ResponseEntity<ResultVO<Nothing>> {
        return ResponseEntity.badRequest().body(ResultVO(code.code, code.message, null))
    }

    fun fail(code: ResponseCode = ResponseCode.FAIL): ResponseEntity<ResultVO<Nothing>> {
        return ResponseEntity.internalServerError().body(ResultVO(code.code, code.message, null))
    }
}