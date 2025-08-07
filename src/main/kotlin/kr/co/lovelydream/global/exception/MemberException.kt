package kr.co.lovelydream.global.exception

import kr.co.lovelydream.global.enums.ResponseCode

class MemberException (
    val code: ResponseCode
) : RuntimeException(code.message)