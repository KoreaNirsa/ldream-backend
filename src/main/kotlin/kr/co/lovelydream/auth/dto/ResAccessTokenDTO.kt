package kr.co.lovelydream.auth.dto

data class ResAccessTokenDTO(
    val accessToken: String,
    val expiresIn: Long
)
