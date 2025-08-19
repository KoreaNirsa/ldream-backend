package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 후 초기 데이터")
data class ResInitDataDTO(
    @Schema(description = "내 닉네임", example = "재섭")
    val myNickname: String,

    @Schema(description = "상대방 닉네임", example = "상대닉네임", nullable = true)
    val partnerNickname: String? = null,

    @Schema(description = "마일리지", example = "1200")
    val mileage: Int,

    @Schema(description = "결제 티어(미구현)", example = "FREE", nullable = true)
    val tier: String? = null,

    @Schema(description = "추억 수(미구현)", example = "0", nullable = true)
    val memoryCount: Int? = null,

    @Schema(description = "AI 추천 여부/데이터(미구현)", example = "null", nullable = true)
    val aiRecommendation: Int? = null
)