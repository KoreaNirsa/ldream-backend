package kr.co.lovelydream.auth.service

import java.util.*

interface TokenStoreService {
    fun saveRefreshToken(userId: String, deviceId: String, refreshJtiOrHash: String, expiredAt: Date)
    fun getRefreshToken(userId: String, deviceId: String): String?
    fun deleteRefreshToken(userId: String, deviceId: String)

    fun blacklistAccessJti(accessJti: String, ttlSeconds: Long)
    fun isAccessBlacklisted(accessJti: String): Boolean

    // (선택) 재사용 감지
    fun markRefreshUsed(refreshJti: String, ttlSeconds: Long)
    fun isRefreshUsed(refreshJti: String): Boolean
}