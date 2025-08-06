package kr.co.lovelydream.global.exception

import kr.co.lovelydream.global.enums.ResponseCode

class AuthException(
    val code: ResponseCode
) : RuntimeException(code.message)