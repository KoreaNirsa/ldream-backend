package kr.co.lovelydream.auth.service

import java.util.*

interface RedisTokenService {
    fun saveRefreshToken(email: String, token: String, expiredAt: Date)

    fun getRefreshToken(email: String): String?

    fun deleteRefreshToken(email: String): Boolean

    fun blacklistAccessToken(token: String, expiredAt: Date)

    fun isBlacklisted(token: String): Boolean
}