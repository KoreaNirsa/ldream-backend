package kr.co.lovelydream.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인/재발급 내부 전달용 토큰 DTO")
data class TokenDTO(
    @get:Schema(description = "액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @get:Schema(description = "리프레시 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...")
    val refreshToken: String
)