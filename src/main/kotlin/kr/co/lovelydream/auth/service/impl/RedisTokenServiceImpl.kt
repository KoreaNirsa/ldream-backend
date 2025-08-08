package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.constants.JwtConstants.ACCESS_BLACKLIST_PREFIX
import kr.co.lovelydream.auth.constants.JwtConstants.REFRESH_PREFIX
import kr.co.lovelydream.auth.service.RedisTokenService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class RedisTokenServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : RedisTokenService {
    private fun keyRefresh(userId: Long, deviceId: String) =
        "${REFRESH_PREFIX}${userId}:${deviceId}"

    override fun saveRefreshToken(userId: Long, deviceId: String, refreshJtiOrHash: String, expiredAt: Date) {
        val ttl = Duration.ofMillis(expiredAt.time - System.currentTimeMillis())
        redisTemplate.opsForValue().set(keyRefresh(userId, deviceId), refreshJtiOrHash, ttl)
    }

    override fun getRefreshToken(userId: Long, deviceId: String): String? =
        redisTemplate.opsForValue().get(keyRefresh(userId, deviceId))

    override fun deleteRefreshToken(userId: Long, deviceId: String) =
        redisTemplate.delete(keyRefresh(userId, deviceId))

    override fun blacklistAccessJti(accessJti: String, ttlSeconds: Long) {
        if (ttlSeconds <= 0) return
        val key = "${ACCESS_BLACKLIST_PREFIX}${accessJti}"
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds))
    }

    override fun isAccessBlacklisted(accessJti: String): Boolean =
        redisTemplate.hasKey("${ACCESS_BLACKLIST_PREFIX}${accessJti}") == true

    override fun markRefreshUsed(refreshJti: String, ttlSeconds: Long) {
        val key = "refresh:used:${refreshJti}"
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds))
    }

    override fun isRefreshUsed(refreshJti: String): Boolean =
        redisTemplate.hasKey("refresh:used:${refreshJti}") == true
}