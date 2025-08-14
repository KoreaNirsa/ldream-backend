package kr.co.lovelydream.auth.service

import java.util.*

interface TokenStoreService {
    fun saveRefreshToken(memberId: String, deviceId: String, refreshJtiOrHash: String, expiredAt: Date)
    fun getRefreshToken(memberId: String, deviceId: String): String?
    fun deleteRefreshToken(memberId: String, deviceId: String)
    fun blacklistAccessJti(accessJti: String, ttlSeconds: Long)
    fun isAccessBlacklisted(accessJti: String): Boolean

    // (선택) 재사용 감지
    fun markRefreshUsed(refreshJti: String, ttlSeconds: Long)
    fun isRefreshUsed(refreshJti: String): Boolean
}