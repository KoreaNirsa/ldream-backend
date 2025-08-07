package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.constants.JwtConstants.BLACKLIST_PREFIX
import kr.co.lovelydream.auth.constants.JwtConstants.REFRESH_PREFIX
import kr.co.lovelydream.auth.service.RedisTokenService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class RedisTokenServiceImpl (
    private val redisTemplate: RedisTemplate<String, String>
) : RedisTokenService {

    override fun saveRefreshToken(email: String, token: String, expiredAt: Date) {
        val key = REFRESH_PREFIX + email
        val ttl = Duration.ofMillis(expiredAt.time - System.currentTimeMillis())
        redisTemplate.opsForValue().set(key, token, ttl)
    }

    override fun getRefreshToken(email: String): String? =
        redisTemplate.opsForValue().get(REFRESH_PREFIX + email)

    override fun deleteRefreshToken(email: String) =
        redisTemplate.delete(REFRESH_PREFIX + email)

    override fun blacklistAccessToken(token: String, expiredAt: Date) {
        val key = BLACKLIST_PREFIX + token
        val ttl = Duration.ofMillis(expiredAt.time - System.currentTimeMillis())
        redisTemplate.opsForValue().set(key, "blacklisted", ttl)
    }

    override fun isBlacklisted(token: String): Boolean =
        redisTemplate.hasKey(BLACKLIST_PREFIX + token) == true
}