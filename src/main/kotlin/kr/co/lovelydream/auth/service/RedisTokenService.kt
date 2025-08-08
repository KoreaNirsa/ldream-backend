package kr.co.lovelydream.auth.service

import java.util.*

interface RedisTokenService {
    fun saveRefreshToken(userId: Long, deviceId: String, refreshJtiOrHash: String, expiredAt: Date)
    fun getRefreshToken(userId: Long, deviceId: String): String?
    fun deleteRefreshToken(userId: Long, deviceId: String): Boolean

    fun blacklistAccessJti(accessJti: String, ttlSeconds: Long)
    fun isAccessBlacklisted(accessJti: String): Boolean

    // (선택) 재사용 감지
    fun markRefreshUsed(refreshJti: String, ttlSeconds: Long)
    fun isRefreshUsed(refreshJti: String): Boolean
}