package kr.co.lovelydream.global.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 응답 포맷")
data class ResultVO<T>(
    @Schema(description = "응답 코드", example = "SUCCESS")
    val code: String,

    @Schema(description = "응답 메시지", example = "정상 처리되었습니다.")
    val message: String,

    @Schema(description = "실제 결과 데이터")
    val result: T? = null
)