package kr.co.lovelydream.auth.dto

data class TokenDTO (
    val accessToken: String,
    val refreshToken: String
)