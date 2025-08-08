package kr.co.lovelydream.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "액세스 토큰 응답 DTO")
data class ResAccessTokenDTO(
    @get:Schema(description = "액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @get:Schema(
        description = "액세스 토큰 만료까지 남은 시간(초). OAuth2 표준 관례로 초 단위 권장",
        example = "1800"
    )
    val expiresIn: Long
)
