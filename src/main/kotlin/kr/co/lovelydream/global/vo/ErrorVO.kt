package kr.co.lovelydream.global.vo

import java.time.OffsetDateTime

data class ErrorVO (
    val timestamp: String = OffsetDateTime.now().toString(),
    val path: String?,
    val code: String,
    val message: String
)